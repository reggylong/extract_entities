
filename = "release/crawl"
path = "inputs/"
n_lines = sum(1 for line in open(filename))


f = open(filename)
workers = 12
outs = [open(path + str(i) + ".in", 'w') for i in xrange(workers)]

k = 0
while k < n_lines:
  line = f.readline()
  outs[k % workers].write(line)
  k += 1
  if k % 1000 == 0:
    print "Counted " + str(k) + " lines"

for x in outs:
  x.close()
f.close()
