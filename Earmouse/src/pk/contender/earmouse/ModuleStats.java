package pk.contender.earmouse;

import android.content.Context;
import android.util.JsonReader;
import android.util.JsonWriter;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Keeps track of all the answers a user has given to exercises of a specific Module.
 * <p>Provides methods for entering answers and obtaining useful information about this data and
 * for reading and writing recorded statistical data to local storage.
 *
 * @author Paul Klinkenberg <pklinken.development@gmail.com>
 */
public class ModuleStats {

    /** All the recorded answers */
    private List<ModuleAnswer> moduleAnswerList;
    /** Reference to local storage */
	private final File statsFile;

	/**
	 * Construct an instance to be associated with the given Module {@link Module#id}, attempts to load from local storage if the file
	 * exists, otherwise creates an fresh instance.
	 * @param context The application context, used for file operations
	 * @param id the ID of the Module this instance will be associated with
	 */
	public ModuleStats(Context context, int id) {

        moduleAnswerList = new ArrayList<>();
		File currentDir = context.getDir("files", Context.MODE_PRIVATE);
		statsFile = new File(currentDir, "stats_" + id + ".json");
		
		if (statsFile.exists()) {
			FileReader fr = null;
			try {
				fr = new FileReader(statsFile);
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
			if(fr != null) {
				try {
					initModuleStatsFromJson(fr);
					fr.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	/**
	 * Reads an existing ModuleStats instance from a JSON file
	 * @param fr The FileReader to read from
	 * @throws IOException
	 */
	private void initModuleStatsFromJson(FileReader fr) throws IOException {
		JsonReader reader = new JsonReader(fr);

		reader.beginArray();
		while (reader.hasNext()) {
			reader.beginObject();
			int exerciseIndex = -1;
			boolean result = true;
			long timestamp = -1;

			while (reader.hasNext()){
				String name = reader.nextName();
                switch (name) {
                    case "exerciseIndex":
                        exerciseIndex = reader.nextInt();
                        break;
                    case "result":
                        result = reader.nextBoolean();
                        break;
                    case "timestamp":
                        timestamp = reader.nextLong();
                        break;
                    default:
                        reader.skipValue();
                        break;
                }
			}
			moduleAnswerList.add(new ModuleAnswer(exerciseIndex, result, timestamp));
			reader.endObject();

		} 
		reader.endArray();
		reader.close();
	}

	/**
	 * Save moduleAnswerList to the given FileWriter as a JSON stream
	 * @param fw The FileWriter to save to
	 * @throws IOException
	 */
	private void saveModuleStatsToJson(FileWriter fw) throws IOException {
		JsonWriter writer = new JsonWriter(fw);

		writer.beginArray();
		for (ModuleAnswer item : moduleAnswerList) {
			writer.beginObject();
			writer.name("exerciseIndex");
			writer.value(item.getExerciseIndex());
			writer.name("result");
			writer.value(item.getResult());
			writer.name("timestamp");
			writer.value(item.getTimestamp());
			writer.endObject();
		}
		writer.endArray();
		writer.close();
	}

	/**
	 * Save this instance to local storage, if the file doesn't exist, creates it, otherwise overwrites existing data.
	 */
	public void saveModuleStats() {

		if (!statsFile.exists()) {
			try {
                //noinspection ResultOfMethodCallIgnored
                statsFile.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		FileWriter fw = null;
		try {
			fw = new FileWriter(statsFile);
		} catch (IOException e) {
			e.printStackTrace();
		}
		if(fw != null)
			try {
				saveModuleStatsToJson(fw);
				fw.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
	}

	/**
	 * Add an answer to {@link #moduleAnswerList}
	 * @param exerciseIndex The index of the exercise the answer refers to
	 * @param result The correctness of the answer
	 */
	public void addAnswer(int exerciseIndex, boolean result) {
		ModuleAnswer answer = new ModuleAnswer(exerciseIndex, result);
		moduleAnswerList.add(answer);
	}

	/**
	 * Calculates the success rate of the registered answers.
	 * @return The percentage of registered answers that is correct or -1 if there is no available data.
	 */
	public int calculateSuccessRate() {

		if(moduleAnswerList.size() == 0)
			return -1;
		else {
			int correctAnswers = 0;
			for (ModuleAnswer answer : moduleAnswerList) {
				if (answer.getResult())
					correctAnswers++;
			}
			return (int)(((float)correctAnswers / (float)moduleAnswerList.size()) * 100);
		}
	}
	
	/**
	 * Returns the total amount of exercises completed
     * <p>An exercise is completed when the user
	 * gave the correct answer, even if there were wrong answers before that.
	 * Since one moves on to the next exercise after giving a correct answer, the number of exercises completed
	 * is the same as the number of correct answers given.
	 * @return the number of exercises in this module that were answered correctly.
	 */
	public int exercisesCompleted() {
		int result = 0;
		
		for (ModuleAnswer answer: moduleAnswerList) {
			if(answer.getResult())
				result++;
		}
		return result;
	}

    /**
     * Returns the success rate in % of a particular exerciseIndex, returns 0 if no exercises were found
     * @param exerciseIndex the individual exercise whose success rate to return
     * @return the success rate in % of the exercise with index exerciseIndex, or 0 if no records were found.
     */
    public int exerciseSuccessRate(int exerciseIndex) {
        int totalCount, correctCount;

        totalCount = correctCount = 0;

        for (ModuleAnswer answer : moduleAnswerList) {
            if(answer.getExerciseIndex() == exerciseIndex) {
                totalCount++;
                if(answer.getResult())
                    correctCount++;
            }
        }

        if(totalCount == 0)
            return 0;
        else
            return (int)(((float)correctCount / (float)totalCount) * 100);
    }

    /**
     * Returns the number of times exerciseIndex is registered.
     * @return the number of times exerciseIndex is registered in {@link #moduleAnswerList}
     */
    public int exerciseCount(int exerciseIndex) {
        int result = 0;

        // TODO: For the purpose of this function, perhaps it is better to only return the succesful answers, worth considering..
        for (ModuleAnswer answer : moduleAnswerList) {
            if(answer.getExerciseIndex() == exerciseIndex)
                result++;
        }

        return result;
    }
	
	/**
	 * Delete all statistical data for this instance.
	 * @return True on success, false otherwise.
	 */
	@SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean purgeStats() {
		
		moduleAnswerList = null;
        return statsFile.exists() && statsFile.delete();
	}
}
