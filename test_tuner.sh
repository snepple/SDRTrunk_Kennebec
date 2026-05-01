for file in $(grep -rl "getTunerIdLabel().setText" ./src/main/java/io/github/dsheirer/source/tuner/); do
    echo "Processing $file"
    # let's replace the string assignment if it's a usb tuner, maybe we can centralize this
done
