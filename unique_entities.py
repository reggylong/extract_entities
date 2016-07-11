import sys
import json
from collections import Counter
from ast import literal_eval

total_extractions = 0 
predicates = set()
extractions_with_pronouns = 0
n_articles = 0
n_sentences = 0

def to_list(relation):
  t = relation[1:-1]
  return t.split(",")

def get_relations(doc):
  global n_sentences
  length = int(doc['length'])
  relations = []
  for i in xrange(length):
    n_sentences += 1
    if str(i) not in doc:
      raise ValueError("Could not find key: " + str(i))
    for x in doc[str(i)]:
      for k, v in x.iteritems():
        if k == 'r' or k == 'relation':
          relations.append(v)
  return relations

args = sys.argv[1:]
print args
pronouns = ['it', 'he', 'she', 'they', 'him', 'her', 'them', 'i', 'me', 'we', 'us', 'you']


subjects = set()
objects = set()
count = 0
for fname in args:
  print "Examining " + fname
  f = open(fname, 'r')
  while True:
    line = f.readline()
    f.readline()
    if line.strip() == "":
      break
    n_articles += 1
    try:
      doc_json = json.loads(line)
      relations = get_relations(doc_json)
      total_extractions += len(relations) 
      for rel in relations:
        r = to_list(rel)
        subjects.add(r[0].lower())
        objects.add(r[2].lower())
    except ValueError:
      pass
    count += 1
    if count % 1000 == 0:
      print "Read " + str(count) +  " lines"

  f.close()
      
entities = subjects.union(objects) 
shared_entities = subjects.intersection(objects)

h = open("entities_output", "w")
h.write("Number of unique subjects: " + str(len(subjects)) + "\n")
h.write("Number of unique objects: " + str(len(objects)) + "\n")
h.write("Size of intersection of subjects and objects:" + str(len(shared_entities)) + "\n")
h.write("Total number of unique entities: " + str(len(entities)) + "\n")
h.write("Total number of sentences: " + str(n_sentences) + "\n")
h.write("Total number of articles: " + str(n_articles) + "\n")

h.close()
