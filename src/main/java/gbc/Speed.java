package gbc;

public class Speed {

    private int speed;
    private int speedCounter;

    public Speed() {
        this(1);
    }

    public Speed(int speed) {
        this.speed = speed;
        this.speedCounter = 0;
    }

    public boolean output() {
        speedCounter = (speedCounter + 1) % speed;
        return speedCounter == 0;
    }

    public void setSpeed(int i) {
        if (i > 0) {
            this.speed = i;
        } else {
            throw new RuntimeException("Can't <= 0");
        }
    }
}
