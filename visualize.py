import sys
import os
from subprocess import call


def main():
    if len(sys.argv) < 4:
        raise "Please specify [jar_path] [root] [output_dir]"
    jar_path, directory, out_dir = sys.argv[1:4]

    sub_dirs = [x for x in os.listdir(directory) if "SPADES" in x and os.path.isdir(os.path.join(directory, x))]

    for sub_dir in sub_dirs:
        call(['mkdir ' + os.path.join(out_dir, sub_dir)], shell=True)

        # Draw 3 month data
        call(['java -Xmx3096m -jar ' + jar_path + ' -i ' + os.path.join(directory, sub_dir, "data", sub_dir) + ' -o ' + os.path.join(out_dir, sub_dir)], shell=True)

        # Draw lab data
        call(['java -Xmx3096m -jar ' + jar_path + ' -l -i ' + os.path.join(directory, sub_dir, "data", sub_dir) + ' -o ' + os.path.join(out_dir, sub_dir)], shell=True)

        # Draw 2-day data
        call(['java -Xmx3096m -jar ' + jar_path + ' -t -i ' + os.path.join(directory, sub_dir, "data", sub_dir) + ' -o ' + os.path.join(out_dir, sub_dir)], shell=True)

if __name__ == "__main__":
    main()
