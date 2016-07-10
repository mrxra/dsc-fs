package li.excessive.fs.meta;

import java.lang.ref.SoftReference;
import java.nio.file.Files;
import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import li.excessive.fs.wrap.WrapFilesystem;
import li.excessive.fs.wrap.WrapPath;


public class MetaDataPath extends WrapPath {
	

	// once accessed, keep soft reference to metadata object...
	// memory should be gc'd when required
	protected SoftReference<MetaDataView> metadataCache = null;

	private static Logger log = LoggerFactory.getLogger(MetaDataPath.class);

	/** 
	 * this constructor is required to make the {@link WrapPath} delegation mechanism work
	 * @param fs a provider specific filesystem extending {@link WrapFilesystem}
	 * @param delegate an ordinary (unwrapped) path object to delegate to
	 */
	public MetaDataPath(WrapFilesystem fs, Path delegate) {
		super(fs, delegate);
	}
	
	protected MetaDataView getMetaDataView() throws Exception {
		MetaDataView b = null;
		if(Files.isRegularFile(this)) {
			if(null == metadataCache || null == metadataCache.get()) {
				log.trace("caching meta data {}", this);
				metadataCache = new SoftReference<MetaDataView>(new MetaDataView(this));
			} else {
				log.trace("metadata loaded from cache {}", this);
			}
			b = metadataCache.get();
		}
		return b;
	}
		
	@Override
	public String toString() {
		return this.toUri().toString();
	}
	

}
