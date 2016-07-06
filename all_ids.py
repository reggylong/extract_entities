import json


f = open("release/crawl")
g = open("ids", "w")

i = 0
ids = []
for line in f:
  if line == "":
    print "empty line"
  else:
    article_json = json.loads(line) 
    g.write(str(article_json["articleId"]) + "\n")
    ids.append(article_json["articleId"])
  i += 1
  if i % 10000 == 0:
    print "Examined " + str(i) + " lines"

print i
g.close()
f.close()

for i in xrange(len(ids) - 1):
  if ids[i] - ids[i+1] != -1:
    print (ids[i], ids[i+1])
