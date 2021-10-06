del /q bootstrap.jar
jar cvf0 bootstrap.jar -C out/production/mytomcat com/hou/mytomcat/Bootstrap.class -C out/production/mytomcat com/hou/mytomcat/myClassLoader/CommonClassLoader.class
del /q lib/mytomcat.jar
cd out
cd production
cd mytomcat
jar cvf0 ../../../lib/mytomcat.jar *
cd ..
cd ..
cd ..
java -cp bootstrap.jar com.hou.mytomcat.Bootstrap
pause