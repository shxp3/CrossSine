package net.ccbluex.liquidbounce.features.module.modules.world

import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.PreUpdateEvent
import net.ccbluex.liquidbounce.event.Render3DEvent
import net.ccbluex.liquidbounce.event.TickEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.features.module.modules.player.AutoItem
import net.ccbluex.liquidbounce.features.module.modules.player.BlockIn
import net.ccbluex.liquidbounce.features.value.BoolValue
import net.ccbluex.liquidbounce.features.value.IntegerRangeValue
import net.ccbluex.liquidbounce.features.value.IntegerValue
import net.ccbluex.liquidbounce.features.value.KeyBindValue
import net.ccbluex.liquidbounce.injection.access.StaticStorage
import net.ccbluex.liquidbounce.ui.client.gui.colortheme.ClientTheme
import net.ccbluex.liquidbounce.utils.ClientUtils
import net.ccbluex.liquidbounce.utils.Rotation
import net.ccbluex.liquidbounce.utils.RotationUtils
import net.ccbluex.liquidbounce.utils.RotationUtils.getRotationDifference
import net.ccbluex.liquidbounce.utils.PlaceRotation
import net.ccbluex.liquidbounce.utils.SlotUtils
import net.ccbluex.liquidbounce.utils.block.BlockUtils
import net.ccbluex.liquidbounce.utils.block.BlockUtils.canBeClicked
import net.ccbluex.liquidbounce.utils.block.BlockUtils.isReplaceable
import net.ccbluex.liquidbounce.utils.block.PlaceInfo
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.ccbluex.liquidbounce.utils.timer.TimeUtils
import net.ccbluex.liquidbounce.utils.timer.TimerMS
import java.awt.Color
import net.minecraft.block.Block
import net.minecraft.block.BlockBed
import net.minecraft.client.settings.GameSettings
import net.minecraft.init.Blocks
import net.minecraft.item.ItemBlock
import net.minecraft.item.ItemStack
import net.minecraft.network.play.client.C0APacketAnimation
import net.minecraft.util.BlockPos
import net.minecraft.util.EnumFacing
import net.minecraft.util.MathHelper
import net.minecraft.util.MovingObjectPosition
import net.minecraft.util.Vec3
import org.lwjgl.input.Keyboard
import kotlin.math.atan2

/**
 * BedDefender — wraps a layered shell of blocks around the nearest bed.
 *
 * Block search and rotation pipeline mirror [net.ccbluex.liquidbounce.features.module.modules.player.BlockIn]:
 *   - Offset sweep (`generateOffsets`) per face, ray-traced for hit validity.
 *   - Best candidate chosen by smallest [getRotationDifference] from current
 *     server rotation, then sent through [RotationUtils.setFreeLookRotation]
 *     with optional smoothing via [RotationUtils.limitAngleChange].
 *
 * Differences from BlockIn:
 *   - Targets come from concentric BFS shells around the nearest bed.
 *   - Strict layer order: shell N is never advanced past while it still has
 *     any replaceable position, even if no clickable neighbour exists this
 *     tick. This keeps the inner walls fully covered before any block is
 *     spent on the outer walls.
 *   - Block tier per layer is auto-derived from hotbar hardness — slowest
 *     to mine (e.g. obsidian) goes innermost, fastest goes outermost.
 *   - Auto-sneak when the click neighbour is a [BlockBed] so the right-click
 *     doesn't trigger sleeping / nether explosion.
 */
@ModuleInfo(name = "BedDefender", category = ModuleCategory.WORLD)
object BedDefender : Module() {

    // ── Settings ────────────────────────────────────────────────────────────
    private val bind = KeyBindValue("Bind", Keyboard.KEY_NONE)
    private val rangeValue = IntegerValue("Range", 5, 1, 10)
    private val layersValue = IntegerValue("Layers", 2, 1, 4)
    /**
     * Layer ordering policy.
     *  - Strict : shell N must be fully filled (or have no reachable face this
     *             tick) before shell N+1 is even considered. Guarantees the
     *             innermost obsidian wall closes before outer endstone goes
     *             down — but a single un-reachable slot stalls the whole module.
     *  - Multi  : every shell is searched together; outer placements proceed
     *             while inner ones are still blocked. Faster coverage but the
     *             outer layers may finish before the inner shell is sealed.
     */
    private val layerModeValue = net.ccbluex.liquidbounce.features.value.ListValue(
        "LayerMode", arrayOf("Strict", "Multi"), "Strict"
    )
    /**
     * How the hotbar material tier is picked for each placement.
     *  - SlowestFirst : always use the slowest-mining block currently in the
     *                   hotbar. Once it runs out, automatically falls through
     *                   to the next slowest. Guarantees every position gets
     *                   the best available material until that material is
     *                   gone — what the user usually wants when stocking
     *                   obsidian + a backup block.
     *  - PerLayer     : original behaviour — shell N uses the N-th slowest
     *                   tier (obsidian inner, endstone next, …). Cheaper
     *                   but mixes materials per layer so a fast block can
     *                   end up on the outer shell while obsidian remains.
     */
    private val blockOrderValue = net.ccbluex.liquidbounce.features.value.ListValue(
        "BlockOrder", arrayOf("SlowestFirst", "PerLayer"), "SlowestFirst"
    )
    private val hitable = BoolValue("Hitable", false)
    private val swingValue = BoolValue("Swing", false)
    private val rotationValue = BoolValue("Rotation", true)
    private val silentRotationValue = BoolValue("Silent-Rotation", true).displayable { rotationValue.get() }
    private val rotSpeedValue = BoolValue("RotationSpeed", true).displayable { rotationValue.get() }
    private val rotationSpeedValue = IntegerRangeValue("MaxRotationSpeed", 180, 0, 0, 180)
    private val silentSwitch = BoolValue("SilentSwitch", true)
    private val blockPlaceDelay = BoolValue("Place-Delay", false)
    private val placeDelay = IntegerRangeValue("MaxPlaceDelay", 0, 0, 0, 1000)

    // ── ESP ─────────────────────────────────────────────────────────────────
    /** Renders a translucent box at every position the module still wants to
     *  fill. When a position transitions from "wanted" → "filled" (e.g. it
     *  just got placed, or the player walked away) the box fades out instead
     *  of popping. */
    private val espValue = BoolValue("ESP", true)
    private val espOutlineValue = BoolValue("ESP-Outline", true).displayable { espValue.get() }
    private val espFadeMsValue = IntegerValue("ESP-FadeOut-MS", 350, 50, 1500).displayable { espValue.get() }

    // ── State ───────────────────────────────────────────────────────────────
    private var rotation: Rotation? = null
    private var targetPlace: PlaceInfo? = null
    private var targetSlot: Int = -1
    private val placeTimer = TimerMS()
    private var active = false
    private var warned = false

    /**
     * Per-position cooldown for placements that the server rejected
     * (`onPlayerRightClick` → false). The position is skipped in [findTarget]
     * until the cooldown expires, so the module immediately moves on to
     * another reachable slot instead of locking onto an unplaceable target
     * tick after tick. 500 ms is enough for a typical server ack of a real
     * placement on an adjacent slot before we retry the failed one.
     */
    private val failedPositions = HashMap<BlockPos, Long>()
    private const val FAIL_COOLDOWN_MS = 500L

    /** Current set of positions the module wants to fill (any layer). Updated
     *  every [findTarget] call. Drives the ESP — entries that drop out of this
     *  set begin fading. */
    private val currentTargets = HashSet<BlockPos>()
    /** Per-position fade alpha 0..1. Pos in [currentTargets] eases up to 1,
     *  pos not in the set eases down to 0 and is removed once invisible. */
    private val fadeMap = HashMap<BlockPos, Float>()
    private var lastFadeTickMs = 0L

    override fun onDisable() {
        active = false
        warned = false
        targetPlace = null
        rotation = null
        targetSlot = -1
        // Safety net: if we were disabled mid-place, restore slot + sneak so
        // the player isn't left holding the wrong item or stuck crouching.
        SlotUtils.stopSet()
        mc.gameSettings.keyBindSneak.pressed = false
        failedPositions.clear()
        currentTargets.clear()
        fadeMap.clear()
        lastFadeTickMs = 0L
    }

    /** True if [pos] is currently in the place-failure cooldown. Also lazily
     *  evicts expired entries on access. */
    private fun isFailed(pos: BlockPos): Boolean {
        val until = failedPositions[pos] ?: return false
        if (System.currentTimeMillis() >= until) {
            failedPositions.remove(pos)
            return false
        }
        return true
    }

    // ── Activation gate (mirrors BlockIn) ───────────────────────────────────
    @EventTarget
    fun onPreUpdate(event: PreUpdateEvent) {
        if (!bind.isKeyDown()) {
            active = false
            warned = false
            targetPlace = null
            rotation = null
            targetSlot = -1
            if (!AutoItem.mining && !BlockIn.active) {
                SlotUtils.stopSet()
            }
            currentTargets.clear()
            return
        }
        if (rankedHotbarBlocks().isEmpty()) {
            active = false
            currentTargets.clear()
            SlotUtils.stopSet()
            if (!warned) {
                ClientUtils.displayAlert("BedDefender : NO BLOCK FOUND")
                warned = true
            }
            return
        }
        active = true
        warned = false

        findTarget()

        // Continuous slot spoof. Hold the placement slot for as long as the
        // module is active, instead of toggling on/off every place() call.
        // Two reasons:
        //  1) When the player is walking toward a target, [findTarget] may
        //     fail to produce a full placement for several ticks (out of
        //     reach). We still want the slot pre-armed so the very first
        //     tick that *does* produce a valid placement can fire the right-
        //     click immediately, without a 1-tick switch delay.
        //  2) [SlotUtils.setSlot] captures `prevSlot` only on first call, so
        //     calling it every tick is cheap and doesn't lose the original
        //     selection — the user's real slot is restored only on
        //     [SlotUtils.stopSet] (bind release / disable / no blocks).
        if (targetSlot != -1) {
            SlotUtils.setSlot(targetSlot, silentSwitch.get(), name)
        }
    }

    private data class HotbarBlock(val block: Block, val slot: Int, val hardness: Float)

    private fun rankedHotbarBlocks(): List<HotbarBlock> {
        val player = mc.thePlayer ?: return emptyList()
        val out = mutableListOf<HotbarBlock>()
        for (slot in 0..8) {
            val stack: ItemStack = player.inventory.getStackInSlot(slot) ?: continue
            val item = stack.item as? ItemBlock ?: continue
            val block = item.block ?: continue
            if (block is BlockBed) continue
            if (!block.isFullCube) continue
            val h = block.getBlockHardness(mc.theWorld, BlockPos.ORIGIN)
            if (h < 0f) continue
            out.add(HotbarBlock(block, slot, h))
        }
        return out.distinctBy { it.block }.sortedByDescending { it.hardness }
    }

    /**
     * Picks the hotbar slot to use for a placement on [layerIdx].
     *
     * In `SlowestFirst` mode the layer index is ignored — every layer
     * receives the slowest-mining block still present in the hotbar.
     * `rankedHotbarBlocks()` only returns slots whose `getStackInSlot`
     * is non-null, so `ranked[0]` is *always* the slowest still-available
     * material. Once it's consumed the next `findTarget` re-ranks and
     * `ranked[0]` naturally becomes the next-slowest block.
     *
     * In `PerLayer` mode we fall back to the original behaviour: shell N
     * gets tier N (clamped to the last available tier so empty slots at
     * the tail don't break placement on outer shells).
     */
    private fun slotForLayer(layerIdx: Int, ranked: List<HotbarBlock>): Int {
        if (ranked.isEmpty()) return -1
        return if (blockOrderValue.equals("SlowestFirst")) {
            ranked[0].slot
        } else {
            ranked[layerIdx.coerceAtMost(ranked.size - 1)].slot
        }
    }

    // ── Bed detection ──────────────────────────────────────────────────────
    private fun findBed(): Pair<BlockPos, BlockPos>? {
        val player = mc.thePlayer ?: return null
        val world = mc.theWorld ?: return null
        val r = rangeValue.get()
        val pPos = BlockPos(player)

        var best: Pair<BlockPos, BlockPos>? = null
        var bestDist = Double.MAX_VALUE

        for (dx in -r..r) for (dy in -r..r) for (dz in -r..r) {
            val pos = pPos.add(dx, dy, dz)
            if (world.getBlockState(pos).block !is BlockBed) continue

            var other: BlockPos? = null
            for (face in EnumFacing.HORIZONTALS) {
                val n = pos.offset(face)
                if (world.getBlockState(n).block is BlockBed) { other = n; break }
            }
            other ?: continue

            val d = player.getDistance(pos.x + 0.5, pos.y + 0.5, pos.z + 0.5)
            if (d < bestDist) { bestDist = d; best = pos to other }
        }
        return best
    }

    private fun buildShells(bed: Set<BlockPos>, layers: Int): List<Set<BlockPos>> {
        val shells = mutableListOf<Set<BlockPos>>()
        val seen = bed.toMutableSet()
        var frontier: Set<BlockPos> = bed

        repeat(layers) {
            val next = mutableSetOf<BlockPos>()
            for (p in frontier) for (f in EnumFacing.values()) {
                val n = p.offset(f)
                if (seen.add(n)) next.add(n)
            }
            shells.add(next)
            frontier = next
        }
        return shells
    }

    private fun findTarget() {
        // Reset the *placement* slot only — rotation is decided below. The
        // place pipeline checks `targetPlace != null` before firing, so
        // clearing it here is enough to prevent a stale tick from re-using
        // last frame's hit vector.
        targetPlace = null
        rotation = null

        val ranked = rankedHotbarBlocks()
        if (ranked.isEmpty()) { currentTargets.clear(); targetSlot = -1; return }

        val bed = findBed() ?: run { currentTargets.clear(); targetSlot = -1; return }
        val bedSet = setOf(bed.first, bed.second)
        val shells = buildShells(bedSet, layersValue.get())
        val strict = layerModeValue.equals("Strict")

        val player = mc.thePlayer
        val occupied: Set<BlockPos> = if (player != null) {
            val feet = BlockPos(player)
            setOf(feet, feet.up())
        } else emptySet()

        // Refresh ESP target set: every position we still want to fill across
        // every shell. Independent of reach — the ESP previews work the user
        // is heading toward even when the spot isn't yet in reach.
        currentTargets.clear()
        for (shell in shells) for (p in shell) {
            if (isReplaceable(p) && p !in occupied) currentTargets.add(p)
        }

        // ── Pass 1: full placement search (reach + raytrace). ──────────────
        // For each shell, try positions in order of distance to the player so
        // the first hit is the closest reachable one — keeps the picked
        // rotation consistent across consecutive ticks (the bot tends to
        // re-select the same neighbour each frame instead of bouncing
        // between two equidistant candidates) and minimises the rotation
        // jump when the player walks across the structure.
        for ((idx, shell) in shells.withIndex()) {
            val unfilled = shell.filter {
                isReplaceable(it) && !isFailed(it) && it !in occupied
            }
            if (unfilled.isEmpty()) continue

            val slot = slotForLayer(idx, ranked)
            if (slot == -1) { targetSlot = -1; return }

            val sorted = if (player != null) {
                unfilled.sortedBy { distSqToBlockCenter(player, it) }
            } else unfilled

            for (pos in sorted) {
                if (search(pos)) {
                    targetSlot = slot
                    return
                }
            }
            // ── Rotation wiggle fallback ────────────────────────────────
            // The geometric face-grid failed for every position in this
            // shell — typically a block partially obstructs the line of
            // sight so none of the fixed grid points raytrace clean. A
            // human solves this by wiggling the crosshair until *some*
            // rotation lands on a valid neighbour face. Replicate that:
            // scan rotation-space around each of the closest positions and
            // accept ANY raytrace hit whose (blockPos, sideHit) resolves to
            // the wanted placement. Because validation IS the same raytrace
            // getMouseOver uses, a hit found here is guaranteed to satisfy
            // the hitable gate once the smoothed rotation converges.
            for (pos in sorted.take(3)) {
                if (wiggleSearch(pos)) {
                    targetSlot = slot
                    return
                }
            }
            if (strict) break
        }

        // ── Pass 2: anticipation rotation. ─────────────────────────────────
        // No fully-valid placement exists this tick — usually because the
        // closest unfilled slot is just out of reach while the player walks
        // toward it. Pre-aim the camera at that slot so the *moment* it
        // enters reach, the very next tick:
        //   - search() succeeds first try (rotation already aligned →
        //     isValidBlockRotation passes immediately)
        //   - place() with hitable=true also passes (mc.objectMouseOver
        //     already on the right block + face)
        // Without this, freelook decays toward camera yaw and there's a
        // 100–300 ms easing-in window where the player is in reach but the
        // module is still rotating — that's the "เดินแล้วไม่วาง" the user
        // reported. Slot is still pre-set in onPreUpdate's continuous
        // setSlot block so we don't add per-tick churn here.
        if (player != null) {
            val nearest = currentTargets
                .asSequence()
                .filter { !isFailed(it) }
                .minByOrNull { distSqToBlockCenter(player, it) }
            if (nearest != null) {
                if (targetSlot == -1) targetSlot = slotForLayer(0, ranked)
                val anticipation = anticipationRotation(player, nearest)
                if (anticipation != null) {
                    rotation = anticipation
                    if (rotationValue.get()) applyRotation(anticipation)
                }
            } else {
                targetSlot = -1
            }
        }
    }

    private fun distSqToBlockCenter(player: net.minecraft.entity.player.EntityPlayer, p: BlockPos): Double {
        val dx = (p.x + 0.5) - player.posX
        val dy = (p.y + 0.5) - (player.entityBoundingBox.minY + player.getEyeHeight())
        val dz = (p.z + 0.5) - player.posZ
        return dx * dx + dy * dy + dz * dz
    }

    /**
     * Best-effort rotation toward [pos] when no full-validity face passes
     * the reach + raytrace check. Picks the clickable neighbour whose face
     * centre is closest to the player's eyes — i.e. the face the player
     * will hit first as they walk in. If no neighbour is currently
     * clickable we still return a rotation aimed at the block centre, so
     * the camera tracks the position smoothly while the world fills in.
     */
    private fun anticipationRotation(player: net.minecraft.entity.player.EntityPlayer, pos: BlockPos): Rotation? {
        val eyes = Vec3(
            player.posX,
            player.entityBoundingBox.minY + player.getEyeHeight(),
            player.posZ
        )

        var bestRot: Rotation? = null
        var bestDist = Double.MAX_VALUE
        for (side in StaticStorage.facings()) {
            val neighbor = pos.offset(side)
            if (!canBeClicked(neighbor)) continue
            val clickFace = side.opposite
            val n = clickFace.directionVec
            val cx = neighbor.x + 0.5 + n.x * 0.5
            val cy = neighbor.y + 0.5 + n.y * 0.5
            val cz = neighbor.z + 0.5 + n.z * 0.5
            val dx = cx - eyes.xCoord
            val dy = cy - eyes.yCoord
            val dz = cz - eyes.zCoord
            val d = dx * dx + dy * dy + dz * dz
            if (d < bestDist) {
                bestDist = d
                bestRot = calculateRotation(eyes, Vec3(cx, cy, cz))
            }
        }
        if (bestRot == null) {
            // Fallback: aim at block centre — keeps the camera following the
            // target visually even when no face is currently clickable.
            bestRot = calculateRotation(eyes, Vec3(pos.x + 0.5, pos.y + 0.5, pos.z + 0.5))
        }
        return bestRot
    }

    /**
     * Shared rotation push pipeline used by both the full-validity search
     * and the anticipation fallback. Identical math to BlockIn so the two
     * modules feel the same when run back-to-back.
     */
    private fun applyRotation(rot: Rotation) {
        var diffAngle = getRotationDifference(RotationUtils.serverRotation, rot)
        if (diffAngle < 0) diffAngle = -diffAngle
        if (diffAngle > 180.0) diffAngle = 180.0

        val rotationSmooth = RotationUtils.limitAngleChange(
            RotationUtils.serverRotation,
            rot,
            ((diffAngle / 360) * rotationSpeedValue.get().last
                + (1 - diffAngle / 360) * rotationSpeedValue.get().first).toFloat()
        )

        if (silentRotationValue.get()) {
            RotationUtils.setTargetRotation(
                if (rotSpeedValue.get()) rotationSmooth else rot, 1
            )
        } else {
            (if (rotSpeedValue.get()) rotationSmooth else rot)!!.toPlayer(mc.thePlayer)
        }
    }

    private fun search(blockPosition: BlockPos): Boolean {
        if (!isReplaceable(blockPosition)) return false
        val player = mc.thePlayer ?: return false
        val reach = mc.playerController.blockReachDistance.toDouble()

        val eyesPos = Vec3(
            player.posX,
            player.entityBoundingBox.minY + player.getEyeHeight(),
            player.posZ
        )

        var placeRotation: PlaceRotation? = null
        var bestScore = Double.MAX_VALUE

        for (side in StaticStorage.facings()) {
            val neighbor = blockPosition.offset(side)
            if (!canBeClicked(neighbor)) continue

            val clickFace = side.opposite
            val n = clickFace.directionVec

            val faceCx = neighbor.x + 0.5 + n.x * 0.5
            val faceCy = neighbor.y + 0.5 + n.y * 0.5
            val faceCz = neighbor.z + 0.5 + n.z * 0.5

            // Cheap face-level cull, but with half-diagonal slack (~0.71)
            // so a face whose *centre* is out of reach while an edge point
            // is still reachable isn't skipped. Per-point reach is checked
            // exactly in the grid loop below.
            val cdx = faceCx - eyesPos.xCoord
            val cdy = faceCy - eyesPos.yCoord
            val cdz = faceCz - eyesPos.zCoord
            val slack = reach + 0.75
            if (cdx * cdx + cdy * cdy + cdz * cdz > slack * slack) continue

            for (a in FACE_STEPS) for (b in FACE_STEPS) {
                val hitVec = when (clickFace.axis) {
                    EnumFacing.Axis.X -> Vec3(faceCx, neighbor.y + a, neighbor.z + b)
                    EnumFacing.Axis.Y -> Vec3(neighbor.x + a, faceCy, neighbor.z + b)
                    EnumFacing.Axis.Z -> Vec3(neighbor.x + a, neighbor.y + b, faceCz)
                    else -> continue
                }

                if (eyesPos.distanceTo(hitVec) > reach) continue

                val rot = calculateRotation(eyesPos, hitVec)
                if (!isValidBlockRotation(neighbor, eyesPos, rot, clickFace, reach)) continue

                val da = a - 0.5
                val db = b - 0.5
                val centerDist = da * da + db * db
                val rotDiff = getRotationDifference(rot).toDouble()
                val score = rotDiff + centerDist * 30.0

                if (score < bestScore) {
                    bestScore = score
                    placeRotation = PlaceRotation(PlaceInfo(neighbor, clickFace, hitVec), rot)
                }
            }
        }

        placeRotation ?: return false

        // Apply rotation through the shared pipeline (same math the
        // anticipation fallback uses).
        rotation = placeRotation.rotation
        if (rotationValue.get()) applyRotation(placeRotation.rotation!!)

        targetPlace = placeRotation.placeInfo
        return true
    }

    private val FACE_STEPS = doubleArrayOf(
        0.5, 0.4, 0.6, 0.3, 0.7, 0.2, 0.8, 0.15, 0.85, 0.1, 0.9
    )

    /** Yaw/pitch offsets (deg) for the rotation wiggle fallback. Centre-out
     *  order so the closest-to-current-aim hit wins the tie-break early. */
    private val WIGGLE_STEPS = floatArrayOf(0f, -4f, 4f, -8f, 8f, -12f, 12f)

    /**
     * Rotation-space fallback for [search]. Instead of picking a point on
     * the face and computing the rotation for it, sweep rotations around
     * the direct line to [blockPosition] and raytrace each one — exactly
     * what getMouseOver will do with that rotation. Accepts any hit whose
     * clicked block + side resolves to placing into [blockPosition], no
     * matter which neighbour face the ray found. Handles partially
     * obstructed sight-lines where only a sliver of some face is visible.
     */
    private fun wiggleSearch(blockPosition: BlockPos): Boolean {
        if (!isReplaceable(blockPosition)) return false
        val player = mc.thePlayer ?: return false
        val world = mc.theWorld ?: return false
        val reach = mc.playerController.blockReachDistance.toDouble()

        val eyesPos = Vec3(
            player.posX,
            player.entityBoundingBox.minY + player.getEyeHeight(),
            player.posZ
        )
        val base = calculateRotation(
            eyesPos, Vec3(blockPosition.x + 0.5, blockPosition.y + 0.5, blockPosition.z + 0.5)
        )

        var best: PlaceRotation? = null
        var bestScore = Double.MAX_VALUE

        for (yawOff in WIGGLE_STEPS) for (pitchOff in WIGGLE_STEPS) {
            val rot = Rotation(
                MathHelper.wrapAngleTo180_float(base.yaw + yawOff),
                (base.pitch + pitchOff).coerceIn(-90f, 90f)
            )
            val dir = RotationUtils.getVectorForRotation(rot)
            val end = eyesPos.addVector(dir.xCoord * reach, dir.yCoord * reach, dir.zCoord * reach)
            val mop = world.rayTraceBlocks(eyesPos, end, false, false, true) ?: continue
            if (mop.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK) continue
            val hitPos = mop.blockPos ?: continue
            val sideHit = mop.sideHit ?: continue
            if (hitPos.offset(sideHit) != blockPosition) continue
            if (!canBeClicked(hitPos)) continue
            if (eyesPos.distanceTo(mop.hitVec) > reach) continue

            val score = getRotationDifference(rot)
            if (score < bestScore) {
                bestScore = score
                best = PlaceRotation(PlaceInfo(hitPos, sideHit, mop.hitVec), rot)
            }
        }

        val placeRotation = best ?: return false
        rotation = placeRotation.rotation
        if (rotationValue.get()) applyRotation(placeRotation.rotation!!)
        targetPlace = placeRotation.placeInfo
        return true
    }

    private fun calculateRotation(eyesPos: Vec3, hitVec: Vec3): Rotation {
        val diffX = hitVec.xCoord - eyesPos.xCoord
        val diffY = hitVec.yCoord - eyesPos.yCoord
        val diffZ = hitVec.zCoord - eyesPos.zCoord
        val diffXZ = MathHelper.sqrt_double(diffX * diffX + diffZ * diffZ).toDouble()
        return Rotation(
            MathHelper.wrapAngleTo180_float((Math.toDegrees(atan2(diffZ, diffX)) - 90).toFloat()),
            MathHelper.wrapAngleTo180_float((-Math.toDegrees(atan2(diffY, diffXZ))).toFloat())
        )
    }

    private fun isValidBlockRotation(
        neighbor: BlockPos, eyesPos: Vec3, rotation: Rotation, facing: EnumFacing, reach: Double
    ): Boolean {
        val rv = RotationUtils.getVectorForRotation(rotation)
        val end = eyesPos.addVector(rv.xCoord * reach, rv.yCoord * reach, rv.zCoord * reach)
        val obj = mc.theWorld.rayTraceBlocks(eyesPos, end, false, false, true) ?: return false
        return obj.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK &&
                obj.blockPos == neighbor &&
                obj.sideHit == facing
    }

    @EventTarget
    fun onTick(event: TickEvent) {
        if (!active) return
        place()
    }

    private fun place() {
        var target = targetPlace ?: return
        val slot = targetSlot
        if (slot == -1) { targetPlace = null; return }
        if (blockPlaceDelay.get() && !placeTimer.hasTimePassed(getDelay)) return
        val mopSneak = mc.objectMouseOver
        if (mopSneak != null && mopSneak.blockPos != null &&
            BlockUtils.getBlock(mopSneak.blockPos) == Blocks.bed) {
            mc.gameSettings.keyBindSneak.pressed = true
        } else {
            mc.gameSettings.keyBindSneak.pressed = GameSettings.isKeyDown(mc.gameSettings.keyBindSneak)
        }
        if (hitable.get()) {
            val mop = mc.objectMouseOver
            if (mop == null || mop.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK) return
            if (mop.blockPos != target.blockPos || mop.sideHit != target.enumFacing) {
                // Opportunistic: while the smoothed rotation is still easing
                // toward the chosen target, the crosshair may already sit on
                // a *different* valid placement (another wanted position, or
                // another face of the same one). Placing there immediately
                // beats waiting for convergence — and is exactly what a
                // human wiggling the mouse does.
                val hitPos = mop.blockPos
                val sideHit = mop.sideHit
                val placeInto = hitPos?.offset(sideHit)
                if (hitPos != null && sideHit != null && placeInto != null &&
                    placeInto in currentTargets && isReplaceable(placeInto) &&
                    !isFailed(placeInto) && canBeClicked(hitPos)
                ) {
                    target = PlaceInfo(hitPos, sideHit, mop.hitVec)
                } else {
                    return
                }
            }
        }

        val player = mc.thePlayer ?: return
        val world = mc.theWorld ?: return
        val controller = mc.playerController ?: return

        // Slot is already armed by onPreUpdate's continuous setSlot — no per-
        // place toggle needed. Restore happens centrally on bind release /
        // disable / no-blocks via SlotUtils.stopSet().
        val ok = controller.onPlayerRightClick(
            player, world, player.inventory.getStackInSlot(slot),
            target.blockPos, target.enumFacing, target.vec3
        )

        if (!ok) {
            // Cooldown keys on the position being placed *into* (neighbour
            // offset by the clicked face) — that's what findTarget filters
            // with isFailed(). Keying on the clicked neighbour itself (old
            // behaviour) never matched, so the cooldown silently did nothing.
            failedPositions[target.blockPos.offset(target.enumFacing)] =
                System.currentTimeMillis() + FAIL_COOLDOWN_MS
        }

        if (ok) {
            if (swingValue.get()) player.swingItem()
            else mc.netHandler.addToSendQueue(C0APacketAnimation())
            placeTimer.reset()
        }
        targetPlace = null
    }

    private val getDelay: Long
        get() = TimeUtils.randomDelay(placeDelay.get().first, placeDelay.get().last)

    // ── ESP rendering ───────────────────────────────────────────────────────

    @EventTarget
    fun onRender3D(event: Render3DEvent) {
        if (!espValue.get() && fadeMap.isEmpty()) return

        val now = System.currentTimeMillis()
        val dtMs = if (lastFadeTickMs == 0L) 16L else (now - lastFadeTickMs).coerceIn(1L, 100L)
        lastFadeTickMs = now

        val fadeMs = espFadeMsValue.get().toFloat().coerceAtLeast(1f)
        val step = dtMs.toFloat() / fadeMs

        // Make sure every active target has an entry so it can fade in.
        for (p in currentTargets) if (p !in fadeMap) fadeMap[p] = 0f

        val it = fadeMap.entries.iterator()
        while (it.hasNext()) {
            val e = it.next()
            val target = if (e.key in currentTargets) 1f else 0f
            val cur = e.value
            val next = if (cur < target) (cur + step).coerceAtMost(target)
                       else              (cur - step).coerceAtLeast(target)
            if (next <= 0.001f && target == 0f) it.remove() else e.setValue(next)
        }

        if (!espValue.get()) return

        val outline = espOutlineValue.get()
        val accent  = ClientTheme.getColor(0)
        for ((pos, alpha) in fadeMap) {
            if (alpha <= 0.01f) continue
            // Eased alpha — smoothstep-ish so the fade reads as a soft pulse
            // rather than a perfectly linear ramp.
            val eased = alpha * alpha * (3f - 2f * alpha)
            val fillA = (eased * 90f).toInt().coerceIn(0, 255)
            val color = Color(accent.red, accent.green, accent.blue, fillA)
            RenderUtils.drawBlockBox(pos, color, outline, !outline,2F, 1F)
        }
    }
}
