package pk.contender.earmouse;

import java.util.ArrayList;
import java.util.List;

/** An abstraction of an exercise, contains a List of exercise units, which are in turn Lists of Integers.
 * <p>
 * An exercise is a List of one or more "exercise units", an exercise unit is a List of one or more samples to be played at once.
 * In an exercise, the exercise units are played one after the other.<br>
 * This enables the following exercise setups:<br>
 * - A single note (for whatever reason): exerciseUnits = { { note } }<br>
 * - A single chord: exerciseUnits = { { note1, note2, note3 } }<br>
 * - A sequence of single notes or chords: exerciseUnits = { { note1, note2, note3 }, { note1, note2, note3 }, {etc} }
 * 
 * @author Paul Klinkenberg <pklinken.development@gmail.com>
 */
public class Exercise {

	/**
	 * Contains the exercise units of this Exercise.
	 * <p>
	 * The values relate to each other as follows:<br>
	 * If exerciseUnits = { { note1, note2, note3 }, { note4, note5, note6 } }<br>
	 * note1 is _always_ 0, this is basically the center of the entire exercise.<br>
	 * note2 and note3 are offsets from note1 and can only be positive.<br>
	 * note4 is the offset from note1(so zero) of the lowest note for this exercise unit.<br>
	 * note5 and note6 are offsets from note4 and can only be positive.<br>
	 * As an example a representation of an Exercise with 2 exercise units, a Major chord and then
	 * the same Major chord played a whole tone lower: <br>
	 * exerciseUnits = { { 0, 4, 7}, {-2, 4, 7} }
	 */
	public final List<List<Integer>> exerciseUnits;
	
	public Exercise() {
		exerciseUnits = new ArrayList<>();
	}

}
