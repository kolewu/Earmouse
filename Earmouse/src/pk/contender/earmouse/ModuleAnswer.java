package pk.contender.earmouse;

import org.joda.time.DateTime;

/**
 * Abstraction of a single answer to an Exercise in a Module. 
 * @author Paul Klinkenberg <pklinken.development@gmail.com>
 */
public class ModuleAnswer {

	/** The index of the Exercise this is an answer of */
	private final int exerciseIndex;
	/** The correctness of the answer */
	private final boolean result;
	/** The time at which the answer was registered */
	private final DateTime timestamp;
	
	public ModuleAnswer(int exerciseIndex, boolean result) {
		this.exerciseIndex = exerciseIndex;
		this.result = result;
		timestamp = new DateTime();
	}
	
	public ModuleAnswer(int exerciseIndex, boolean result, long timestamp) {
		this.exerciseIndex = exerciseIndex;
		this.result = result;
		this.timestamp = new DateTime(timestamp);
	}
	
	public int getExerciseIndex() {
		return exerciseIndex;
	}

	public boolean getResult() {
		return result;
	}

	public long getTimestamp() {
		return timestamp.getMillis();
	}

}
