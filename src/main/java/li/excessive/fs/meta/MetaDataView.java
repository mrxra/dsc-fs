package li.excessive.fs.meta;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.file.attribute.UserDefinedFileAttributeView;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Date;
import java.util.Formatter;
import java.util.LinkedList;
import java.util.List;
import java.util.zip.CRC32;

import org.apache.tika.Tika;
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.metadata.TikaCoreProperties;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.sax.BodyContentHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * http://www.awaresystems.be/imaging/tiff/faq.html#q3
 */
public class MetaDataView implements UserDefinedFileAttributeView {

	private static Logger log = LoggerFactory.getLogger(MetaDataView.class);
	
	protected static final String NAME = "meta";
	
	private Metadata metadata = null;

	protected MetaDataPath path = null;
	
	private Tika tika = null;
	
	@SuppressWarnings("unused")
	public MetaDataView(MetaDataPath path) throws Exception {
		this.path = path;
		this.tika = new Tika();
        this.metadata = new Metadata();
        try (InputStream stream = TikaInputStream.get(this.path.getBytes(), metadata)) {
        	String mimeType = tika.detect(stream, metadata);
        	Parser parser = new AutoDetectParser();
        	parser.parse(stream, new BodyContentHandler(), metadata, new ParseContext());
        }
   	}
	
	@Override
	public String name() {
		return NAME;
	}

	@Override
	public List<String> list() {
		List<String> attrs = new LinkedList<>();
		Collections.addAll(attrs, metadata.names());
//		Comparator<String> b = (String p, String q) -> p.compareTo(q);
		Collections.sort(attrs);
		return attrs;
	}

	@Override
	public int size(String name) throws IOException {
		throw new UnsupportedOperationException("not (yet) immplemented.");
	}
	
	@Override
	public int read(String name, ByteBuffer dst) throws IOException {
		throw new UnsupportedOperationException("not (yet) immplemented.");
	}

	@Override
	public int write(String name, ByteBuffer src) throws IOException {
		throw new UnsupportedOperationException("read-only meta data access");
	}

	@Override
	public void delete(String name) throws IOException {
		throw new UnsupportedOperationException("read-only meta data access");
	}
	
	public List<String> read(String name) {
		List<String> values = new LinkedList<>();
		if(metadata.isMultiValued(name)) {
			Collections.addAll(values, metadata.getValues(name));
		} else {
			values.add(metadata.get(name));
		}
		return values;
	}
	
	/**
	 * generates the md5 hash value of the file
	 * @return alphanumeric hash value
	 */
	public String getMd5() {
		String md5 = null;
		try {
			MessageDigest md = MessageDigest.getInstance("MD5");
			md.update(path.getBytes());
			md5 = new BigInteger(1, md.digest()).toString(16);
		} catch (Exception e) {
			log.warn("failed to calculate md5 sum of {}", path, e);
		}
		return md5;
	}

	/**
	 * generates the sha1 hash value of the file
	 * @return alphanumeric hash value
	 */
	public String getSha1() {
		String sha1 = null;
		try(Formatter f = new Formatter()) {
			MessageDigest sha = MessageDigest.getInstance("SHA-1");
			sha.update(path.getBytes());
		    for (byte b : sha.digest()) {
		        f.format("%02x", b);
		    }
		    sha1 = f.toString();
		} catch (Exception e) {
			log.warn("failed to calculate md5 sum of " + path, e);
		}
		return sha1;
	}
	
	
	/**
	 * CRC-32 checksum
	 * @return
	 * @throws IOException
	 */
	public String getCrc32() throws IOException {
		CRC32 crc32 = new CRC32();
		crc32.update(path.getBytes());
		return Long.toHexString(crc32.getValue());
	}
	
	/**
	 * mime type (e.g. image/jpeg). alias of 'Content-Type'
	 * @return
	 */
	public String getMimeType() {
		return metadata.get(Metadata.CONTENT_TYPE);
	}
	
	/**
	 * extract filename extension (the part of the filename after the last '.' character)
	 */
	public String getFilenameExtension() {
		String fn = path.getFileName().toString();
		return fn.contains(".") ? fn.replaceAll("^.*\\.([^\\.]+)$", "$1") : null;
	}
	
	/**
	 * tries to determine a date/time when the file was originally created. 
	 * for jpeg images with exif header this is the corresponding 'DateTimeOriginal' field,
	 * aggregated with the value 'SubSecTimeOriginal' (indicating the fraction of seconds)
	 * if present.
	 * @return
	 */
	public LocalDateTime getDateTimeCreated() {
		LocalDateTime ldt = null;
		log.trace("determine creation date of {}", path);
		// try date/time original first...works for all dsc images with exif header...
		if(null == ldt) {
			try {
				// TODO: does tika provide these as property constants...?
				String dt = metadata.get("Date/Time Original");
				String ms = metadata.get("Sub-Sec Time");
				// add faction of seconds if appropriate field is set...
				dt = dt + "." + String.format("%1$-3s", (null != ms ? ms : "")).replace(' ', '0');
				DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy:MM:dd HH:mm:ss.SSS");
				ldt = LocalDateTime.parse(dt, formatter);
				log.trace("...found date/time original");
			} catch (Exception e) {
				log.trace("...date/time original not found");
			}
		}
		// ...otherwise try the dublin core creation date determined by tika...this takes care
		// of movie files recorded on a digital still camera, where date/time extraction isn't quite
		// standardised apparently...but tika seems to cope just fine with mjpeg files
		if(null == ldt) {
			try {
				Date dt = metadata.getDate(TikaCoreProperties.CREATED); //dcterms:created
				Instant instant = Instant.ofEpochMilli(dt.getTime());
				ldt = LocalDateTime.ofInstant(instant, ZoneId.systemDefault()); 
				
				// TODO: is there any use-case for this? if we have a sub-sec time, it's certainly
				// in an exif header and therefore we should already have read the date/time original?
//				String ms = metadata.get("Sub-Sec Time");
//				if(null != ms) {
//					log.warn("read ms from file without exif header (!)...", this.path);
//					String n = String.format("%1$-9s", (null != ms ? ms : "")).replace(' ', '0');
//					int ns = Integer.parseInt(n); // x 10^6 -> ns
//					ldt = ldt.withNano(ns);
//				}
				log.trace("...found dublin core creation date");
			} catch (Exception e) {
				log.trace("...dublin core creation date not found");
			}
		}
		return ldt;
	}

	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		for(String n : this.list()) {
			sb.append(n).append(" : ").append(this.read(n)).append("\n");
		}
		return sb.toString();
	}
	
}
