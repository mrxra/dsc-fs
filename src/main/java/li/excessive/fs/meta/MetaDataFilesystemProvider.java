package li.excessive.fs.meta;

import java.nio.file.FileStore;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttributeView;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import li.excessive.fs.wrap.WrapFileStore;
import li.excessive.fs.wrap.WrapFilesystem;
import li.excessive.fs.wrap.WrapFilesystemProvider;
import li.excessive.fs.wrap.WrapPath;


public class MetaDataFilesystemProvider extends WrapFilesystemProvider {

	private static Logger log = LoggerFactory.getLogger(MetaDataFilesystemProvider.class);
	
	protected WrapPath wrap(Path path) {
		return new MetaDataPath(this.register(path.getFileSystem()), path);
	}
	protected WrapFileStore wrap(FileStore filestore) {
		return new MetaDataFileStore(filestore);
	}
	
	protected WrapFilesystem wrap(FileSystem delegate, Map<String, ?> env) {
		return new MetaDataFilesystem(this, delegate, env);
	}

	@Override
	public String getScheme() {
		return "meta";
	}

	@SuppressWarnings("unchecked")
	@Override
	public <V extends FileAttributeView> V getFileAttributeView(Path path, Class<V> type, LinkOption... options) {
		V view = null;
		try {
			if(MetaDataView.class == type) {
				MetaDataPath mdp = (MetaDataPath)path;
				view = (V)mdp.getMetaDataView();
			} else {
				view = Files.getFileAttributeView(this.unwrap(path), type, options);
			}
		} catch (Exception e) {
			// some files might not support the requested attribute view...in which case null should be returned
			// ...so we just trace the exception here and ignore it
			log.trace("attribute view " + type.getCanonicalName() + " not supported for file " + path, e);
		}
		return view;
	}

}
