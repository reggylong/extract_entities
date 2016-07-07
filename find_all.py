import sys
import json
import os

def get_sentence_info(article, i):
  sentence = ""
  relations = set()
  for x in article[str(i)]:
    for k, v in x.iteritems():
      if k == 'r':
        relations.add(v)
      elif k == 's':
        sentence = v
  return (sentence, relations)

def search(article, e1=None, e2=None):
  length = int(article['length'])
  find_e2 = False if e2 is None else True 
  find_e1 = False if e1 is None else True

  pairs = []
  count = 0
  for i in xrange(length):
    found_relations = []
    sentence, relations = get_sentence_info(article, i)
    for relation in relations:
      found_e1 = False
      found_e2 = False
      relation = relation.lower()
      if (not find_e1 and not find_e2):
        found_relations.append(relation)
        count += 1
        continue
      if e1.lower().encode('utf-8') in relation:
        found_e1 = True
      if find_e2 and (e2.lower() in relation):
        found_e2 = True
      if ((found_e1 and found_e2) or (found_e1 and not find_e2)):
        found_relations.append(relation)
        count += 1

    pairs.append((sentence, found_relations))

  return (count, pairs)

def main():
  f = open(sys.argv[1], 'r')
  g = open(os.path.basename(sys.argv[1]) + "_search", 'w')
  line = ""
  newline_count = 0
  total = 0
  while True:
    article = ""
    while True:
      line = f.readline()
      if line.strip() == "":
        newline_count += 1
        break
      newline_count = 0
      article += line
    if newline_count > 1:
      break
    try:
      article_json = json.loads(article)
      count, pairs = search(article_json, "obama", "minneapolis")
      if count == 0:
        continue
      g.write("ARTICLE ID: " + article_json['articleId'] + article_json['date']  + '\n')
      for sentence, relations in pairs:
        if len(relations) == 0:
          continue
        g.write("Sentence: " + sentence.encode('utf-8') + "\n")
        for relation in relations:
          g.write(relation.encode('utf-8') + "\n")
        g.write("\n")
    except ValueError:
      pass
    total += 1
    if total % 1000 == 0:
      print "Read " + str(total) + " examples"
  g.close()
  f.close()

main()
