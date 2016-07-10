package li.excessive.fs.meta;

import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import li.excessive.fs.wrap.WrapFilesystem;

public class MetaDataFilesystem extends WrapFilesystem {

	@SuppressWarnings("unused")
	private static Logger log = LoggerFactory.getLogger(MetaDataFilesystem.class);
	
	public MetaDataFilesystem(MetaDataFilesystemProvider provider, FileSystem delegate, Map<String, ?> env) {
		super(provider, delegate, env);
	}
	
	@Override
	public Path getPath(String first, String... more) {
		return new MetaDataPath(this, delegate.getPath(first, more));
	}
	
	@Override
	public Iterable<Path> getRootDirectories() {
		Iterable<Path> itr = StreamSupport
			.stream(delegate.getRootDirectories().spliterator(), false)
			.map(it -> new MetaDataPath(this, it))
			.collect(Collectors.toList());
		return itr;
	}

	@Override
	public Set<String> supportedFileAttributeViews() {
		Set<String> vs = delegate.supportedFileAttributeViews();
		vs.add(MetaDataView.NAME);
		return vs;
	}


}
