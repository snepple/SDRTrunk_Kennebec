#!/bin/bash
sed -i '/setFrequency(config.getFrequency());/i \            setAutoOptimizeSampleRate(config.isAutoOptimizeSampleRate());' ./src/main/java/io/github/dsheirer/source/tuner/TunerController.java
