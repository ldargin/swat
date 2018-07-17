package Engine.enginePackage;

import java.util.Random;

/** A simple Random subclass which allows to get the seed state. */
public class CustomRandom extends Random {
	private static final long serialVersionUID = 1L;
	
	private final static long multiplier = 0x5DEECE66DL;
    private final static long addend = 0xBL;
    private final static long mask = (1L << 48) - 1;

	private long seed;
	
	/** creates a random generator with the given seed. */
	public CustomRandom(long seed){
		setSeed((seed ^ multiplier) & mask);
	}

	/** Sets a seed value obtained with {@link #getSeed()}. */
	@Override
	public void setSeed(long seed){
		this.seed = seed;
	}
	
	/** Returns the current seed of the random generator. */
	public long getSeed() {
		return seed;
	}
	
	/** Calculates the next seed. */
	@Override
    protected int next(int bits) {
	    seed = (seed * multiplier + addend) & mask;
        return (int)(seed >>> (48 - bits));
    }
	
}
