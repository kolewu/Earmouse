package pk.contender.earmouse;

import android.content.Context;
import android.util.JsonReader;
import android.util.JsonWriter;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Abstraction of a set of exercises that belong together, e.g. all basic major intervals.
 * <p>
 * Manages a module's data and state, generates exercises and has several I/O methods
 * for reading and writing Module objects.
 * @author Paul Klinkenberg <pklinken.development@gmail.com>
 */
public class Module implements Comparable<Module>{

	/** Constants referring to the difficulty of a Module */
	final static public int DIFF_BEGINNER = 1, DIFF_AMATEUR = 2, DIFF_INTERMEDIATE = 3, DIFF_EXPERT = 4;
	
	private final Context mCtx;
	/** The unique ID of this Module */
	private int id;
	/** The title of this Module */
	private String title;
	/** The description of this Module's contents or purpose */
	private String description;
	/** The lowest and highest notes this Module is allowed to use in its exercises (lowestNote, highestNote)
	 * In these variable, 0 refers to C2 and 41 to E5 */
	private int lowestNote, highestNote;
	/** The difficulty of this Module */
	private int difficulty;
    /** Reference to this Module's statistics */
	private ModuleStats stats;
	/** The version of create_module.py used to create this Module */
	private String toolVersion;

    /** Module version number
     * <p>
     * Not currently used for anything, planned use is for Module updates.
     */
    private int moduleVersion = 1;
    /** A short description on the Module contents
     * <p>
     * Used in ListViews to give the user an indication of a Module's contents beyond the title
     */
    private String shortDescription;

	/** List of the answers for this Module's exercises */
	private List<String> answerList = new ArrayList<>();
	/** List of this Module's Exercises */
	private final List<Exercise> exerciseList = new ArrayList<>();

    /**
     * Contructs an empty (and useless) Module
     * <p>Used when a fully initialised Module object is not required (e.g. ListViews)
     */
	public Module (Context context) {
		
		mCtx = context;
		id = -1;
		title = description = shortDescription = null;
		lowestNote = highestNote = -1;
		answerList = null;
		difficulty = -1;
		toolVersion = "n/a";
        moduleVersion = 1;
	}

	/**
	 * Constructs a Module by reading a JSON from the given filename
	 * @param context The application context
	 * @param moduleFile The File from which to read the JSON data
	 */
	public Module (Context context, File moduleFile) {
		mCtx = context;
		try {
			FileInputStream fis = new FileInputStream(moduleFile);
			FileReader filereader = new FileReader(fis.getFD());
			initModuleFromJson(filereader);
			filereader.close();
			fis.close();
		} catch (IOException e) {
			e.printStackTrace();

        }
		stats = new ModuleStats(mCtx, id);
	}

	/**
	 * Constructs a Module by reading a JSON from the given InputStreamReader
	 * @param context The application context
	 * @param reader The Reader from which to read the JSON data
	 */
	public Module(Context context, InputStreamReader reader) {
		mCtx = context;

		try {
			initModuleFromJson(reader);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Save a Module's statistics
	 * @see ModuleStats#saveModuleStats()
	 */
	public void saveState () {
        stats.saveModuleStats();
	}

	/**
	 * Reload this Module's statistics 
	 */
	public void refreshState() {
		stats = new ModuleStats(mCtx, id);
	}

	/**
	 * Loads this Module's properties and data from the JSON data of the given Reader
	 * @param r The Reader from which to read the JSON data
	 * @throws IOException
	 */
	private void initModuleFromJson(Reader r) throws IOException {
		JsonReader reader = new JsonReader(r);

		reader.beginObject();
		while (reader.hasNext()) {
			String name = reader.nextName();
            switch (name) {
                case "moduleId":
                    this.id = reader.nextInt();
                    break;
                case "title":
                    this.title = reader.nextString();
                    break;
                case "description":
                    this.description = reader.nextString();
                    break;
                case "shortDescription":
                    this.shortDescription = reader.nextString();
                    break;
                case "lowestNote":
                    this.lowestNote = reader.nextInt();
                    break;
                case "highestNote":
                    this.highestNote = reader.nextInt();
                    break;
                case "difficulty":
                    this.difficulty = reader.nextInt();
                    break;
                case "version":
                    this.toolVersion = reader.nextString();
                    break;
                case "moduleVersion":
                    this.moduleVersion = reader.nextInt();
                    break;
                case "exerciseList":
                    reader.beginArray();
                    for (int i = 0; reader.hasNext(); i++) {
                        this.exerciseList.add(new Exercise());
                        reader.beginArray();
                        for (int j = 0; reader.hasNext(); j++) {
                            this.exerciseList.get(i).exerciseUnits.add(new ArrayList<Integer>());
                            reader.beginArray();
                            while (reader.hasNext()) {
                                this.exerciseList.get(i).exerciseUnits.get(j).add(reader.nextInt());
                            }
                            reader.endArray();
                        }
                        reader.endArray();
                    }
                    reader.endArray();
                    break;
                case "answerList":
                    reader.beginArray();
                    while (reader.hasNext()) {
                        this.answerList.add(reader.nextString());
                    }
                    reader.endArray();
                    break;
                default:
                    reader.skipValue();
                    break;
            }
		}
		reader.endObject();
		reader.close();
	}

    /**
     * Generate a random number between [0 - limit>, with a linearly descending distribution from 0 to limit.
     * <p>Example distribution of 10000 calls with limit == 5:
     * Occurrences of 0: 3322
     * Occurrences of 1: 2630
     * Occurrences of 2: 2016
     * Occurrences of 3: 1371
     * Occurrences of 4: 661
     *
     * @param limit the upper limit of return values.
     * @return a random number between [0 - limit> in a linearly descending distribution from 0 to limit.
     */
    private int getLinearRandomNumber(int limit) {
        Random rng = new Random();
        int randomMultiplier = limit * (limit + 1) / 2;
        int randomNumber = rng.nextInt(randomMultiplier);

        int result = 0;
        for(int i = limit; randomNumber >= 0; i--) {
            randomNumber -= i;
            result++;
        }
        return result - 1;
    }

	/**
	 * Returns an index to one of this Module's exercises that is random but weighted towards certain properties.
     * <p>
     * Specifically, it sorts all available exercises first on success rate and then on how often they were attempted.
     * It then uses {@link #getLinearRandomNumber} to pick one, thus preferring items higher on the list.
     *
	 * @return A weighted index to one of this Module's exercises.
	 */
	public int getWeightedExerciseIndex() {

        /**
         * Combines an exercise with the current success rate and the frequency of occurrences.
         * Allows sorting exercises based on those factors.
         * Implemented Comparable sorted on success rate first and if equal sorts on frequency of occurrence.
         */
        //noinspection NullableProblems
        class ratedExercise implements Comparable<ratedExercise> {
            int exerciseIndex;
            int successRate;
            int exerciseCount;

            ratedExercise(int exerciseIndex, int successRate, int exerciseCount) {
                this.exerciseIndex = exerciseIndex;
                this.successRate = successRate;
                this.exerciseCount = exerciseCount;
            }

            int getSuccessRate() { return successRate; }
            int getExerciseIndex() { return exerciseIndex; }
            int getExerciseCount() { return exerciseCount; }

            @Override
            public int compareTo(ratedExercise another) {

                if (this.getSuccessRate() == another.getSuccessRate())
                    return this.getExerciseCount() - another.getExerciseCount();
                else
                    return this.getSuccessRate() - another.getSuccessRate();
            }
        }

        List<ratedExercise> ratedExerciseList = new ArrayList<>();
        for(int index = 0; index < exerciseList.size(); index++) {
            ratedExerciseList.add(new ratedExercise(index, stats.exerciseSuccessRate(index), stats.exerciseCount(index)));
        }
        Collections.sort(ratedExerciseList);
        // ratedExerciseList is now the list of all exercises in this module sorted by success rate and count

        return ratedExerciseList.get(getLinearRandomNumber(ratedExerciseList.size())).getExerciseIndex();
	}

	/**
	 * @return The Module's title
	 */
	public String getTitle() {
		return title;
	}

	/**
	 * @return The Module's description
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * @return The Module's difficulty
	 */
	public int getDifficulty() {
		return difficulty;
	}

	/**
	 * @return The Module's answer list
	 */
	public List<String> getAnswerList() {
		return answerList;
	}

	/**
	 * Generates an Exercise that can be used by {@link pk.contender.earmouse.MediaFragment} to generate
     * a WAV sample.
	 * <p> 
	 * The Exercise objects in {@link #exerciseList} are an abstract representation of a sequence of notes/chords.
     * This function maps the Exercise at the given index to a random point between {@link #lowestNote} and {@link #highestNote}.
     * The result can be used by {@link pk.contender.earmouse.MediaFragment} to generate a WAV sample.
     * @param exerciseIndex The index of the Exercise to generate.
	 * @return An Exercise instance that can be used to prepare a WAV sample.
	 */
	public Exercise getExercise(int exerciseIndex) {
		Exercise resultExercise = new Exercise();
		
		int positiveOffset = 0;
		int negativeOffset = 0;
		for(List<Integer> exerciseUnit : exerciseList.get(exerciseIndex).exerciseUnits) {
			if(exerciseUnit.get(0) < negativeOffset)
				negativeOffset = exerciseUnit.get(0);
			int span = 0;
			for(int i = 1;i < exerciseUnit.size(); i++) {
				if(exerciseUnit.get(i) > span) 
					span = exerciseUnit.get(i);
			}
			if((exerciseUnit.get(0) + span) > positiveOffset)
				positiveOffset = (exerciseUnit.get(0) + span);
		}
		// At this point negativeOffset is the largest negative offset to be found in this exercise
		// and positiveOffset is the largest positive offset.
		
		// So now we can generate a random baseOffset that will not exceed the bounds of highestNote
		// or lowestNote as prescribed by the Module.
		
		Random rng = new Random();
		int delta = (highestNote - positiveOffset) - (lowestNote - negativeOffset);
		int baseOffset = rng.nextInt(delta) + (lowestNote - negativeOffset);
		
		Log.d("Debug", "exerciseIndex: " + exerciseIndex + " positiveOffset:" + positiveOffset
				+ " negativeOffset:" + negativeOffset + " delta: " + delta + "baseOffset: " + baseOffset);
		
		// Now we can derive the values for the resultExercise using the baseOffset
		for(int i = 0;i < exerciseList.get(exerciseIndex).exerciseUnits.size(); i++) {
			resultExercise.exerciseUnits.add(new ArrayList<Integer>());
			resultExercise.exerciseUnits.get(i).add(baseOffset + exerciseList.get(exerciseIndex).exerciseUnits.get(i).get(0));
			for(int j = 1;j < exerciseList.get(exerciseIndex).exerciseUnits.get(i).size(); j++) {
				resultExercise.exerciseUnits.get(i).add(resultExercise.exerciseUnits.get(i).get(0) + exerciseList.get(exerciseIndex).exerciseUnits.get(i).get(j)); 
			}
		}
		
		return resultExercise;
	}
	
	/**
	 * Register an answer with the ModuleStats instance
	 * @param exerciseIndex Index of the Exercise we are registering an answer for
	 * @param result The correctness of the answer
	 */
	public void registerAnswer(int exerciseIndex, boolean result) {
		stats.addAnswer(exerciseIndex, result);
	}

	/**
	 * @return The success rate for this Module
	 * @see ModuleStats#calculateSuccessRate()
	 */
	public int getSuccessRate() {
		return stats.calculateSuccessRate();
	}

	/**
	 * @return the Module ID
	 */
	public int getId() {
		return id;
	}

	/**
	 * Set the Module unique ID
	 * @param id The ID to set
	 */
	public void setId(int id) {
		this.id = id;
	}

	/**
	 * Set the Module title
	 * @param title The title to set
	 */
	public void setTitle(String title) {
		this.title = title;
	}

	/**
	 * Set the Module difficulty
	 * @param difficulty The difficulty to set
	 */
	public void setDifficulty(int difficulty) {
		this.difficulty = difficulty;
	}

   	/**
	 * Attempts to write this Module to the device's local storage.
	 * <p>
	 * Write this Module to disk as a JSON file, does not save the value of ModuleStats
	 * @return True on success, false otherwise (this happens if an exception occurs)
	 */
	public boolean writeModuleToJson() {
		
		File currentDir = mCtx.getDir("files", Context.MODE_PRIVATE);
		File modFile = new File(currentDir, "module" +
				Main.getLocaleSuffix() + "_" +
				id + ".json");
		
		if (!modFile.exists()) {
			try {
                //noinspection ResultOfMethodCallIgnored
                modFile.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
				return false;
			}
		}
		FileWriter fw;
		try {
			fw = new FileWriter(modFile);
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
        try {
            JsonWriter writer = new JsonWriter(fw);

            writer.beginObject();
            writer.name("moduleId");
            writer.value(this.getId());
            writer.name("title");
            writer.value(this.getTitle());
            writer.name("description");
            writer.value(this.getDescription());
            writer.name("shortDescription");
            writer.value(this.getShortDescription());
            writer.name("lowestNote");
            writer.value(this.lowestNote);
            writer.name("highestNote");
            writer.value(this.highestNote);
            writer.name("difficulty");
            writer.value(this.getDifficulty());
            writer.name("version");
            writer.value(this.getToolVersion());
            writer.name("moduleVersion");
            writer.value(this.getModuleVersion());
            writer.name("answerList");
            writer.beginArray();
            for(String answer : answerList) {
                writer.value(answer);
            }
            writer.endArray();
            writer.name("exerciseList");
            writer.beginArray();
            for(Exercise exercise : exerciseList) {
                writer.beginArray();
                for(List<Integer> exerciseUnit : exercise.exerciseUnits) {
                    writer.beginArray();
                    for(Integer value : exerciseUnit) {
                        writer.value(value);
                    }
                    writer.endArray();
                }
                writer.endArray();
            }
            writer.endArray();
            writer.endObject();
            writer.close();
            fw.close();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        // Since we changed the local contents we should reload Main.mModules.
        Main.refreshModuleList(mCtx);
		
		return true;
	}

	/**
	 * Remove this Module and its associated ModuleStats file from local storage
	 * @return True if successful, false otherwise.
	 */
	@SuppressWarnings("UnusedReturnValue")
    public boolean purgeModule() {
		File currentDir = mCtx.getDir("files", Context.MODE_PRIVATE);
        File modFile = new File(currentDir, "module" +
                Main.getLocaleSuffix() + "_" +
                id + ".json");
		
		if(!stats.purgeStats())
			Log.d("DEBUG", "stats.purgeStats() returned false");

        return modFile.exists() && modFile.delete();
    }

    /**
     * Reset the statistics for this Module
     */
    public void resetStats() {
        if(stats != null)
            if(!stats.purgeStats())
                Log.d("DEBUG", "Error deleting statistics");
        stats = new ModuleStats(mCtx, id);
    }

  	/**
	 * Comparable implementation, sorts first on difficulty, then on title.
	 * @see Comparable
	 */
	@Override
	public int compareTo(@SuppressWarnings("NullableProblems") Module another) {
		
		if(this.getDifficulty() == another.getDifficulty()) {
			// Modules have same difficulty, subsort by title
			return this.getTitle().compareToIgnoreCase(another.getTitle());
		} else
			return this.getDifficulty() - another.getDifficulty(); 	
	}
	
	/**
	 * Returns the version number of the create_module.py tool that was used to create this module
	 * @return the version number of the create_module.py tool that was used to create this module
	 */
    String getToolVersion() {
		return this.toolVersion;
	}
	
	/**
	 * @see ModuleStats#exercisesCompleted
	 */
	public int getExercisesCompleted() {
		return stats.exercisesCompleted();
	}

    public String getShortDescription() {
        return shortDescription;
    }

    public void setShortDescription(String shortDescription) { this.shortDescription = shortDescription; }

    int getModuleVersion() {
        return moduleVersion;
    }

    public void setModuleVersion(int moduleVersion) {
        this.moduleVersion = moduleVersion;
    }
}
