package net.ccbluex.liquidbounce.utils;


public final class Translate {
    private boolean AckerRunCRACKED_f;
    private float AckerRunCRACKED_a;
    private float AckerRunCRACKED_ALLATORIxDEMO;

    public float getX() {
        Translate IIiiiiiiIiIii = this;
        return IIiiiiiiIiIii.AckerRunCRACKED_a;
    }

    public static String AckerRunCRACKED_ALLATORIxDEMO(String IIiiiiiiIiIii) {
        int n = IIiiiiiiIiIii.length();
        int n2 = n - 1;
        char[] cArray = new char[n];
        int n3 = 1 << 3 ^ 4;
        int cfr_ignored_0 = 3 << 3 ^ 1;
        int n4 = n2;
        int n5 = 2 << 3 ^ 3;
        while (n4 >= 0) {
            int n6 = n2--;
            cArray[n6] = (char)(IIiiiiiiIiIii.charAt(n6) ^ n5);
            if (n2 < 0) break;
            int n7 = n2--;
            cArray[n7] = (char)(IIiiiiiiIiIii.charAt(n7) ^ n3);
            n4 = n2;
        }
        return new String(cArray);
    }

    /*
     * WARNING - void declaration
     */
    public Translate(float f, float f2) {
        float IIiiiiiiIiIii = 0;
        Translate IIiiiiiiIiIii2 = this;
        Translate translate = IIiiiiiiIiIii2;
        IIiiiiiiIiIii2.AckerRunCRACKED_f = false;
        translate.AckerRunCRACKED_a = IIiiiiiiIiIii;
        translate.AckerRunCRACKED_ALLATORIxDEMO = f2;
    }

    public float getY() {
        Translate IIiiiiiiIiIii = this;
        return IIiiiiiiIiIii.AckerRunCRACKED_ALLATORIxDEMO;
    }


    public void setX(float IIiiiiiiIiIii) {
        this.AckerRunCRACKED_a = IIiiiiiiIiIii;
    }

    public void setY(float IIiiiiiiIiIii) {
        this.AckerRunCRACKED_ALLATORIxDEMO = IIiiiiiiIiIii;
    }
}
