import sys
import json
from collections import Counter
from ast import literal_eval
import re

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
    if str(i) in doc:
      for x in doc[str(i)]:
        for k, v in x.iteritems():
          if k == 'r' or k == 'relation':
            relations.append(v)
    else:
      raise ValueError("Key not found: " + str(i))
  return relations

args = sys.argv[1:]
print args
pronouns = ['it', 'he', 'she', 'they', 'him', 'her', 'them', 'i', 'me', 'we', 'us', 'you']


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
        predicates.add(r[1].lower())
        sub_tokens = set(re.split('\W+', r[0].lower()))
        obj_tokens = set(re.split('\W+', r[2].lower()))
        for x in pronouns:
          if x in sub_tokens or x in obj_tokens:
            extractions_with_pronouns += 1
            break
    except ValueError:
      print "Error at line: " + str(count)
      pass
    count += 1
    if count % 1000 == 0:
      print "Read " + str(count) +  " lines"

  f.close()
      
print("Total extractions: " + str(total_extractions))
print("Number of unique predicates: " + str(len(predicates)))
print("Number of extractions with pronouns: " + str(extractions_with_pronouns))
print("Total number of sentences: " + str(n_sentences))
print("Total number of articles: " + str(n_articles))

