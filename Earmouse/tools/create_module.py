#!/usr/bin/env python3

# Tool to create module JSONs for the Earmouse android app
# After its input will write a JSON to "module_<ID>.json"
# lowestNote and highestNote have boundarier of 0 and 41,
# the 0 being the C2 and 41 the E5

__version__ = "0.3"
__author__ = "Paul Klinkenberg"

lowest_note_default = 0
highest_note_default = 41

import json, os

class Module():

    def __init__(self, module_id, title, description, short_description, lowest_note, highest_note, exercise_list, answer_list, difficulty, module_version):
        self.module_id = module_id
        self.title = title
        self.description = description
        self.short_description = short_description
        self.lowest_note = lowest_note
        self.highest_note = highest_note
        self.exercise_list = exercise_list
        self.answer_list = answer_list
        self.difficulty = difficulty
        self.module_version = module_version

    def dump_json(self):
        with open("module_" + str(self.module_id) + ".json", "w") as f:
            json.dump({"moduleId" : self.module_id, "title": self.title, "description" : self.description,
            "shortDescription" : self.short_description, "lowestNote" : self.lowest_note, 
            "highestNote" : self.highest_note, "exerciseList" : self.exercise_list, 
            "answerList" : self.answer_list, "difficulty" : self.difficulty, "version" : __version__,
            "moduleVersion" : self.module_version}, f)


def safe_int_input(question):
    
    while True:
        try:
            output = int(input(question))
            break
        except ValueError:
            print("Invalid input, try again...")
    return output

#Should generate a unique ID somehow but for now we just look at the amount of modules
#in the working direction and suggest using that+1
files_list = os.listdir()
module_count = 0
for item in files_list:
    if "module_" in item:
        module_count += 1
module_count += 1
module_version = 1
print("Module version defaulting to {}".format(module_version))
id_answer = input("Enter module ID(default={}): ".format(module_count))
module_id = int(id_answer) if id_answer != "" else module_count
title = input("Enter module title: ")
description = input("Give module description: ")
short_description = input("Give short description(used in module list): ")
ln_answer = input("What is the lowest note an exercise can use (default={}) ? ".format(lowest_note_default))
lowest_note = int(ln_answer) if ln_answer != "" else lowest_note_default
hn_answer = input("What is the highest note an exercise can use (default={}) ? ".format(highest_note_default))
highest_note = int(hn_answer) if hn_answer != "" else highest_note_default
exercise_amount = safe_int_input("How many different exercises will there be in this module ? ")
difficulty = safe_int_input("What is the difficulty level (1-4) ? ")

print("\nExercise/answer pairs:")
print("Exercises can be of arbitary length, to end unit input just enter an empty line")
print("The first value of an exercise should always be 0, this is the base for the other values")
exercise_list = []
answer_list = []
for i in range(exercise_amount):
    exercise_units = []

    while True:
        unit = input("Please enter the values of unit {} in exercise {}: ".format(len(exercise_units), i))
        if unit == "":
            if len(exercise_units) == 0:
                continue
            else:
                break
        unit_int = [int(elem) for elem in unit.split(",")]
        exercise_units.append(unit_int)

    if exercise_units[0][0] != 0:
        print("First element of first exercise unit should be 0, aborting")
        exit(1)
    exercise_list.append(exercise_units)
    al_answer = input("Please give the correct answer for this exercise: ")
    answer_list.append(al_answer)
   
   # Done with data entry, construct object:

mod = Module(module_id, title, description, short_description, lowest_note, highest_note,
exercise_list, answer_list, difficulty, module_version)
mod.dump_json()
print("done")
