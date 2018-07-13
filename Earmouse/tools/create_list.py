#!/usr/bin/env python3

#Tool to create a list JSON of all the module JSONS in the current directory
#for use with the Earmouse Android app
#will write a file module_list.json to the current directory that contains:
#Module title, Module id, Module difficulty.

__version__ = "0.2"
__author__ = "Paul Klinkenberg"

import json, os

class ModuleListItem():
    def __init__(self, module_title, module_id, difficulty, short_description, module_version):
        self.module_title = module_title
        self.module_id = module_id
        self.difficulty = difficulty
        self.short_description = short_description
        self.module_version = module_version


def load_module_json(item):
    print("loading {}".format(item))
    with open(item, "r") as f:
        module = json.load(f)
    module_list.append(ModuleListItem(module["title"], module["moduleId"], 
    module["difficulty"], module["shortDescription"], module["moduleVersion"]))

def encode_module(obj):
    if isinstance(obj, ModuleListItem):
        return obj.__dict__
    return obj

def write_modulelist_json(module_list):
    with open("list.json", "w") as f:
        #json.dump(f, module_list, default=encode_module)
        json.dump([item.__dict__ for item in module_list], f)

module_list = []
files_list = os.listdir()
for item in files_list:
    if "module_" in item:
        load_module_json(item)

write_modulelist_json(module_list)
print("created list.json with {} items".format(len(module_list)))
