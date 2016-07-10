package li.excessive.fs;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.Assert;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;

public class Env extends TestWatcher {

	private Description desc = null;
	
	public Env() { 
		super();
	}
	
	@Override
	protected void starting(Description description) {
		this.desc = description;
		try {
			if(Files.exists(this.output())) { 
				Files.walkFileTree(this.output(), new DeleteOp()); 
			}
			Files.createDirectories(this.output());
		} catch (IOException e) {
			Assert.fail();
		}
	}
	
	public Path root() {
		return Paths.get(System.getProperty("user.dir"));
	}
	public Path resource(String s) {
		return this.root().resolve("src/test/resources").resolve(s);
	}
	public Path output() {
		return output(null);
	}
	public Path output(String s) {
		Path workspace = this.root().resolve("build").resolve("test-output").resolve(desc.getClassName() + "." + desc.getMethodName());
		return (null != s ? workspace.resolve(s) : workspace);
	}

}
