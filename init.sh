#!/bin/bash



#mkdir Reverb && cd Reverb && wget http://reverb.cs.washington.edu/reverb-latest.jar && jar xf reverb-latest.jar && cd ..
#mkdir classes

mkdir CoreNLP &&
  wget http://nlp.stanford.edu/software/stanford-corenlp-full-2015-12-09.zip &&
  unzip stanford-corenlp-full-2015-12-09.zip &&
  rm -rf stanford-corenlp-full-2015-12-09.zip

for file in stanford-corenlp-full-2015-12-09/*.jar; do
  mv $file CoreNLP
done

rm -rf stanford-corenlp-full-2015-12-09 &&

  cd CoreNLP &&
  for file in *.jar; do
    jar xf $file
  done
