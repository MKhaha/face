## ubuntu上运行
### 克隆工程
cd ~
git clone https://github.com/MKhaha/face.git
### 创建文件夹
cd ~
mkdir faceLocal
cd ./faceLocal
mkdir arcsoft-lib
mkdir static
mkdir test
cd static
mkdir facePhoto
mkdir importantPerson
mkdir original
### 拷贝库文件
cd ~/face/lib/ArcSoft_ArcFace_Java_Linux_x64_V2.2/libs/LINUX64
cp * ~/faceLocal/arcsoft-lib/
### 安装依赖包
mvn install:install-file -Dfile=/root/face/lib/ArcSoft_ArcFace_Java_Linux_x64_V2.2/libs/arcsoft-sdk-face-2.2.0.1.jar -DgroupId=com.arcsoft.face -DartifactId=arcsoft-sdk-face -Dversion=2.2.0.1 -Dpackaging=jar
### 安装nginx
### 配置环境变量
vim ~/.bashrc，增加export FACELOCAL=/root/faceLocal/
source ~/.bashrc
### 启动docker
service docker start（stop/restart）
### 编译工程，运行
mvn clean install docker:build
###
docker run -it --privileged=true --env FACELOCAL=/root/faceLocal/ -v /root/faceLocal/:/root/faceLocal/:ro -p 8081:8081 mydocker/face

docker run -it --env FACELOCAL=/root/faceLocal/ -v /root/faceLocal/:/root/faceLocal/ -p 8081:8081 mydocker/face

docker run -d -p 8081:8080 springboot-demo

docker run -it --privileged=true -p 9999:8080 --mount type=bind,source=/root/logs,target=/logs springboot-demo


将Dockerfile和jar包放在一起，使用下列命令也可以打包
docker build -t xxx .