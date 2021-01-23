#! /usr/bin/bash
#
#  Copyright (C) 2020  Edward Lynch-Milner
#
#  This program is free software: you can redistribute it and/or modify
#  it under the terms of the GNU General Public License as published by
#  the Free Software Foundation, either version 3 of the License, or
#  (at your option) any later version.
#
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
#
# You should have received a copy of the GNU General Public License
#  along with this program.  If not, see <https://www.gnu.org/licenses/>.
#

#This script is to be used to attempt to download the latest jar/specified release jar or just add encrypt file to existing built jar

# clean up if an error occurs
# $1 is the jar file
# $2 if not left out is the password.encrypt file
function unsuccessfulCleanUp() {
  rm -f "$1" > /dev/null
  if [ -n "$2" ]; then
    rm -f "$2"

    if [ "$?" -ne "0" ]; then
      echo "Warning, password.encrypt file could not be deleted from the output directory, it should be deleted for security"
    fi
  fi
}

function printHelp() {
  printf "SimpleFTP Installation Script\n"
  printf "\t-v\t<version>\tThe release version to download and install\n"
  printf "\t-j\t<jar_file>\tAn existing JAR file that you want to install\n"
  printf "\t-o\t[output_dir]\tWhere to place the installed jar. Defaults to current working directory\n"
  printf "\t-h\t\t\tPrints this help info\n"
  printf "\t-javafx\t[directory]\tThe directory where JavaFX runtime is installed\n\t\t\t\tIf you get errors running the simple_ftp script like module path requires module specification, try explicitly stating the path here\n\t\t\t\tOtherwise attempts to use PATH_TO_FX\n"
  printf "\t-d\tEnables debugging to be enabled in the running program\n"
  printf "\t-i\tInstalls the JavaFX runtime to the same directory as the JAR file and the script\n"
  printf "\nIf -v or -j are specified, only one of each other may be used. If neither are specified, this downloads the latest release\n"
}

# Confirms if an existing JAR should be deleted
# Arg 1 is the name of the jar
function confirmExistingJarRemoval() {
  jar_to_remove="$1"

  read -p "Confirm removal of JAR $1? (Y/N) " choice

  while [ "$choice" != "Y" ] && [ "$choice" != "N" ];
  do
    read -p "Confirm removal? (Y/N) " choice
  done

  if [ "$choice" == "Y" ]; then
    rm -f "$jar_to_remove"
  fi
}

usage="./simpleftp_install.sh [ -v <version> | -j <jar-file> ] [-o*utput directory] [-h*elp] [-javafx installationDir] [-d*ebug] [-i*nstall_FX_Runtime]"

ARG_COUNT=$#

version=""
jar_file=""
output_dir="$(pwd)"
javafx_install="$PATH_TO_FX"
javafx_path_specified="false"
debug_param=""
install_fx_runtime="false"
javafx_version="11.0.2"

while [ "$ARG_COUNT" -gt "0" ];
do
  case "$1" in
    -v ) version="$2"
         shift 2
         ARG_COUNT=$((ARG_COUNT - 2))
         ;;
    -j ) jar_file="$2"
         shift 2
         ARG_COUNT=$((ARG_COUNT - 2))
         ;;
    -o ) output_dir="$2"
         shift 2
         ARG_COUNT=$((ARG_COUNT - 2))
         ;;
    -h ) printHelp
         exit 0
         ;;
    -javafx ) javafx_install="$2"
              javafx_path_specified="true"
              shift 2
              ARG_COUNT=$((ARG_COUNT - 2))
              ;;
    -d ) debug_param="-Dsimpleftp.debug"
        shift
        ARG_COUNT=$((ARG_COUNT - 1))
        ;;
    -i ) install_fx_runtime="true"
          shift
          ARG_COUNT=$((ARG_COUNT - 1))
          ;;
    * ) echo "$usage"
        exit 1
        ;;
  esac
done

echo "[SimpleFTP Installation]"

SECONDS=0

get_latest_version="true"

if [ -n "$version" ] || [ -n "$jar_file" ]; then
  get_latest_version="false"
fi

echo -e "\n\t[Input Validation]\n"

echo "Validating if -v and -j flags are used correctly"

if [ -n "$version" ] && [ -n "$jar_file" ]; then
  echo "You cannot specify both version and jar file, exiting..."
  exit 3
fi

echo "Validating if -javafx and -i flags are used correctly"

if [ "$javafx_path_specified" == "true" ] && [ "$install_fx_runtime" == "true" ]; then
    echo "Conflicting arguments, cannot specify a path to JavaFX Runtime and also request to install it, exiting..."
    exit 4
fi

if [ "$javafx_path_specified" == "true" ] && [ -z "$javafx_install" ]; then
  echo "-javafx flag specified but no path was given, attempting to use environment variable PATH_TO_FX"
  javafx_install="$PATH_TO_FX"
fi

echo "Checking if output directory exists"

if [ ! -d "$output_dir" ] && [ ! -f "$output_dir" ]; then
  read -p "$output_dir does not exist. Do you want to create it? (Y/N) " choice

  while [ "$choice" != "Y" ] && [ "$choice" != "N" ];
  do
    read -p "Do you want to create it? (Y/N) " choice
  done

  if [ "$choice" == "Y" ]; then
    echo "Creating $output_dir"
    mkdir "$output_dir" || exit
  else
    echo "Aborting installation..."
    exit 0
  fi
fi

echo "Checking if output directory is accessible"

if [ -f "$output_dir" ] || [ ! -x "$output_dir" ] || [ ! -w "$output_dir" ]; then
  echo "$output_dir is not accessible, exiting..."
  exit 5
fi

echo "Checking if Java is installed"

java_command=$(command -v java)

if [ -z "$java_command" ]; then
  echo "Java is not installed, you need at least JRE 11 to run this application, exiting..."
  exit 14
fi

if [ -n "$jar_file" ]; then
  if [[ "$jar_file" != "/"* ]]; then
    jar_file="$(pwd)/$jar_file" # make absolute before we change working directory
  fi
fi

echo -e "\n\t[Set up installation environment]\n"

echo "Changing directory to $output_dir"

cd "$output_dir"
succeeded=$?

if [ "$succeeded" -ne "0" ]; then
  echo "Changing to $output_dir failed, exiting..."
  exit 6
fi

echo "Changed to $output_dir successfully"

echo -e "\n\t[Download Artifacts]\n"

files_downloaded="false"

if [ "$get_latest_version" == "true" ]; then
  files_downloaded="true"
  echo "Retrieving latest version from repository"

  LIB_TAGS=$(curl -i https://api.github.com/repos/edwardUL99/simple-ftp/tags 2> /dev/null | grep name)

  if [ -z "$LIB_TAGS" ]; then
    echo "There are no available release candidates to download, exiting..."
    exit 2
  fi

  OLD_IFS=$IFS
  IFS=","
  read -ra TAGS <<< "$LIB_TAGS"
  version=$(echo "${TAGS[0]}" | awk '{print $2}' | tr -d '"')
  IFS=$OLD_IFS

  echo "Retrieved latest version as $version"
fi

if [[ "$jar_file" != /* ]]; then
    # not absolute
    jar_file="$(pwd)/$jar_file"
fi

if [ -n "$version" ]; then
  files_downloaded="true"
  echo -e "\nDownloading the JAR file from release version $version from the repo"
  download="wget https://github.com/edwardUL99/simple-ftp/releases/download/$version/simple-ftp-${version}.jar"
  $download

  if [ "$?" -ne "0" ] || [ ! -f "simple-ftp-${version}.jar" ]; then
    echo "Download of the JAR failed as version $version does not exist, or another error, exiting..."
    exit 7
  fi

  jar_file="$(pwd)/simple-ftp-${version}.jar"
  echo "Downloaded JAR file to $jar_file"
fi

javafx_sdk_installed=$(find . -name "javafx-sdk-$javafx_version" 2> /dev/null)

if [ "$install_fx_runtime" == "true" ]; then
  files_downloaded="true"

  echo "-i flag specified to install JavaFX Runtime $javafx_version"

  if [ -n "$javafx_sdk_installed" ]; then
    echo "JavaFX runtime $javafx_version already present in $output_dir, aborting JavaFX Runtime download"
  else
    echo -e "Downloading JavaFX runtime $javafx_version to $output_dir\n"
    download_version=$(echo $javafx_version | tr . -)
    download="wget -O javafx.zip https://gluonhq.com/download/javafx-$download_version-sdk-linux/"
    $download

    if [ "$?" -ne "0" ]; then
      echo "Download of JavaFX Runtime failed, exiting..."
      exit 8
    fi

    unzip javafx.zip
    unzipped=$?
    rm -f javafx.zip

    if [ "$unzipped" -ne "0" ]; then
      echo "Extracting JavaFX Runtime failed, exiting..."
      exit 8
    fi

    echo "JavaFX Runtime $javafx_version installed to $(pwd)/javafx-sdk-$javafx_version. Will use this runtime for installation"
  fi
fi

if [ "$files_downloaded" == "false" ]; then
  echo "Nothing to download"
fi

echo -e "\n\t[Installation]\n"

echo "Checking if there are any existing simple-ftp JARS in $output_dir"

mapfile -t files < <(find . -name "simple-ftp*.jar")

for f in "${files[@]}";
do
  confirmExistingJarRemoval "$f"
done

if [ "${#files[@]}" -eq "0" ]; then
  echo "No pre-existing simple-ftp JARs passencrypt_found"
fi

if [ -n "$jar_file" ]; then
  echo "Installing JAR file $jar_file in $output_dir"

  if [ -z "$version" ]; then
    echo "Copying $jar_file to $output_dir"
    cp "$jar_file" .

    succeeded="$?"
    if [ "$succeeded" -ne "0" ]; then
      echo "Failed to copy JAR $jar_file to $output_dir, exiting..."
      exit 9
    fi

    jar_file="$(pwd)/$(basename $jar_file)"
  fi

  echo -e "\n\t[password.encrypt Generation]\n"

  echo "Generating the password.encrypt file used for password encryption"
  echo "It is not recommended to change this file after installation as otherwise saved passwords will no longer be able to be de-crypted"

  key=$(LC_ALL=C tr -dc 'A-Za-z0-9!"#$%&'\''()*+,-./:;<=>?@[\]^_`{|}~' < /dev/urandom | head -c 20)

  if [ -z "$key" ]; then
    echo "Failed to generate password key, exiting..."
    unsuccessfulCleanUp $jar_file
    exit 10
  fi

  echo "$key" > password.encrypt

  add_command=""
  list_command=""
  properties_extract_command=""
  zip_used="false"

  jar_command=$(command -v jar)
  zip_command=$(command -v zip)
  unzip_command=$(command -v unzip)

  properties_path="$(pwd)/simpleftp.properties"

  if [ -n "$jar_command" ]; then
    add_command="$jar_command -uf $jar_file password.encrypt"
    list_command="$jar_command -tf $jar_file | grep -x password.encrypt"
    properties_extract_command="$jar_command -xf $jar_file simpleftp.properties"
  elif [ -n "$zip_command" ] && [ -n "$unzip_command" ]; then
    add_command="$zip_command -u $jar_file password.encrypt"
    list_command="$unzip_command -l $jar_file"
    properties_extract_command="$unzip_command $jar_file simpleftp.properties"
    zip_used="true"
  else
    echo "Missing commands required for installing JAR file. At a minimum, if you don't have a JDK, you need zip and unzip commands, exiting..."
    unsuccessfulCleanUp "$jar_file" password.encrypt
    exit 11
  fi

  $add_command
  success=$?

  if [ "$success" -ne "0" ]; then
    echo "Failed to add password.encrypt to the JAR file, exiting..."
    unsuccessfulCleanUp $jar_file password.encrypt
    exit 12
  fi

  jar_listing=$($list_command)
  passEncrypt_found=""

  if [ "$zip_used" == "false" ]; then
    passEncrypt_found=$(echo "$jar_listing" | grep -x password.encrypt)
  else
    passEncrypt_found=$(echo "$jar_listing" | awk '{print $4}' | grep -x password.encrypt)
  fi

  if [ -z "$passEncrypt_found" ]; then
    echo "password.encrypt file could not be found inside $jar_file, exiting..."

    unsuccessfulCleanUp $jar_file password.encrypt

    exit 13
  else
    rm -f password.encrypt
    if [ "$?" -ne "0" ]; then
      echo "Warning, password.encrypt file not successfully removed from the output directory. It should be removed for security"
    fi

    echo "JAR file $jar_file successfully initialised with password.encrypt file"

    echo -e "\n\t[Runtime properties setup]\n"

    if [ -f "$properties_path" ]; then
      echo "A properties file already exists, backing up to $properties_path~"
      cp "$properties_path" "$properties_path~"
    fi

    echo "Extracting simpleftp.properties from JAR to $output_dir"
    $properties_extract_command
    succeeded=$?

    if [ "$succeeded" -ne "0" ]; then
      echo "Failed to extract properties file, exiting..."
      exit 16
    fi

    echo "Properties installed to $properties_path"

    if [ -n "$debug_param" ]; then
      echo "Setting the -Dsimpleftp.debug property"
    fi

    echo -e "\n\t[JavaFX Runtime Setup]\n"

    use_installed_sdk="false"

    unset choice

    if [ -n "$javafx_sdk_installed" ] && [ "$install_fx_runtime" == "false" ]; then
      read -p "JavaFX $javafx_version Runtime has been found in $output_dir. Would you like to use this installation? (Y/N) " choice

      while [ "$choice" != "Y" ] && [ "$choice" != "N" ];
      do
        read -p "Would you like to use this installation? (Y/N) " choice
      done
    fi

    if [ "$choice" == "Y" ] || [ "$install_fx_runtime" == "true" ]; then
      use_installed_sdk="true"
      echo "Using JavaFX Runtime $javafx_version found in $output_dir"
    elif [ "$choice" == "N" ]; then
      echo "Using JavaFX Runtime specified by PATH_TO_FX"
    fi

    if [ "$install_fx_runtime" == "true" ] || [ "$use_installed_sdk" == "true" ]; then
      local_install="$(pwd)/javafx-sdk-$javafx_version/lib"
      echo "Setting JavaFX installation path locally to $local_install"
      export PATH_TO_FX="$local_install"
    else
      echo "Setting JavaFX installation path to $javafx_install"
      export PATH_TO_FX="$javafx_install"
    fi

    echo -e "\n\t[Script Installation]\n"

    run_script="$(pwd)/simple_ftp"

    echo "Creating simple_ftp run script"
    echo -e "#! /usr/bin/bash\n\n# This run-script is generated automatically by the simpleftp_install.sh script\n\njava --module-path $PATH_TO_FX --add-modules=javafx.controls $debug_param -Dsimpleftp.properties=$properties_path -jar $jar_file" > "$run_script"
    succeeded="$?"

    if [ "$succeeded" -ne "0" ]; then
      echo "Creation of simple_ftp script failed, exiting..."
      exit 15
    fi

    echo "Giving the script execute permissions"
    chmod +x "$run_script"
    succeeded="$?"

    if [ "$succeeded" -ne "0" ]; then
      echo "Warning, could not make the simple_ftp script executable. You will have to run it by calling bash simple_ftp, or else try manually running chmod +x $run_script"
    fi

    echo -e "\n\t[Finished]\n"
    elapsed="$SECONDS"

    echo -e "Installation completed in $elapsed seconds\n"

    echo -e "SimpleFTP installed to $run_script\nYou can copy the simple_ftp script to another location provided the installation artifacts (JAR and if specified, JavaFX) remain in the same location"
  fi
fi
