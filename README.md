# batching-algorithm-semiconductor-manufacturing-implementation
# Description du problème:
  
## Upgrade to Java 21

This repository is a small Java project without an existing build system. To standardize builds and target the latest LTS (Java 21), a minimal Maven `pom.xml` has been added that configures the compiler to use Java 21.

What I did:
- Added a `pom.xml` configured to compile with Java 21.

How to prepare your environment (Windows / PowerShell):

1. Install JDK 21 (Temurin / Adoptium or Oracle/OpenJDK builds). For example, download from https://adoptium.net or https://jdk.java.net/21/ and install.

2. Verify Java and javac point to the JDK 21 installation:

	```powershell
	java -version
	javac -version
	```

3. If you prefer to build with Maven (recommended once installed):

	```powershell
	mvn -v
	mvn -DskipTests package
	```

	The `pom.xml` already sets the compiler `source`, `target` and `release` to 21.

4. Or compile and run without Maven using the JDK 21 tools directly:

	```powershell
	# compile all .java files to the current folder
	javac --release 21 -d out *.java
	# run the application (replace Job with the class containing main)
	java -cp out Job
	```

Notes:
- If your system `java`/`javac` still point to an older JDK, update your PATH or JAVA_HOME to the JDK 21 installation.
- Because this project had no existing build file, I added a minimal Maven POM. If you prefer Gradle or another build tool, I can add that instead.

If you'd like, I can attempt to run a local build and fix any compilation issues next.

