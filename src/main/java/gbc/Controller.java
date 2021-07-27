package gbc;

public class Controller {

    /**
     * Current state of the buttons, bit set = pressed.
     */
    protected int buttonState;
    protected boolean p10Requested;

    // RIGHT LEFT UP DOWN A B SELECT START (0-7)

    public void buttonDown(int buttonIndex) {
        buttonState |= 1 << buttonIndex;
        p10Requested = true;
    }

    public void buttonUp(int buttonIndex) {
        buttonState &= 0xff - (1 << buttonIndex);
        p10Requested = true;
    }

}
