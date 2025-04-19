
#!/bin/bash
echo "Doing Fresh Code Checkout for ................. fvp-service"

cd /apps/fvp-service/code/

rm -rf /apps/fvp-service/code/fvp-service

git clone -b main https://github.com/tymbldev/fvp-service.git

cd /apps/fvp-service/code/fvp-service

git checkout main
echo "Successfully Fresh Code Checkout for ................. fvp-service"

echo "Doing Maven build for repo............ fvp-service"
mvn clean install -DskipTests
mv /apps/fvp-service/code/fvp-service/target/*.jar /apps/fvp-service/code/fvp-service/target/services.jar
echo "Successfully Maven build for repo............ fvp-service"

[ -d /apps/fvp-service/fvp-service ] || mkdir /apps/fvp-service/fvp-service
[ -d /apps/fvp-service/logs/fvp-service ] || mkdir /apps/fvp-service/logs/fvp-service

echo "Copying jar to /apps/fvp-service/fvp-service folder"
cp /apps/fvp-service/code/fvp-service/target/services.jar /apps/fvp-service/
echo "Successfully Copying jar to /apps/fvp-service/fvp-service folder"

echo "Restarting service via supervisor with fvp-service"

supervisorctl restart fvp-service

echo "Successfully Restarting service via supervisor with name fvp-service"

echo "Doing Folder cleanup.................. "
rm -rf /apps/fvp-service/code/fvp-service
echo "Successfully Folder cleanup.................. /apps/fvp-service/code/fvp-service"
