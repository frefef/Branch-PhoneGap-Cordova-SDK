package io.branch.referral;

/**
 * ApkParser
 *
 * <P>Parses the 'compressed' binary form of Android XML docs, such as AndroidManifest.xml, that
 * are contained within an APK file.
 *
 * @author Alex Austin
 */
public class ApkParser {
	// decompressXML -- Parse the 'compressed' binary form of Android XML docs 
	// such as for AndroidManifest.xml in .apk files
	public static int endDocTag = 0x00100101;
	public static int startTag =  0x00100102;
	public static int endTag =    0x00100103;

    /**
     * Returns the result of decompression of XML as a {@link String}.
     * @param xml A {@link Byte[]} containing the XML to be decompressed.
     * @return A {@link String} containing the result of the decompression action.
     */
	public String decompressXML(byte[] xml) {
		// Compressed XML file/bytes starts with 24x bytes of data,
		// 9 32 bit words in little endian order (LSB first):
		//   0th word is 03 00 08 00
		//   3rd word SEEMS TO BE:  Offset at then of StringTable
		//   4th word is: Number of strings in string table
		// WARNING: Sometime I indiscriminently display or refer to word in 
		//   little endian storage format, or in integer format (ie MSB first).
		int numbStrings = LEW(xml, 4*4);
	
		// StringIndexTable starts at offset 24x, an array of 32 bit LE offsets
		// of the length/string data in the StringTable.
		int sitOff = 0x24;  // Offset of start of StringIndexTable
	
		// StringTable, each string is represented with a 16 bit little endian 
		// character count, followed by that number of 16 bit (LE) (Unicode) chars.
		int stOff = sitOff + numbStrings*4;  // StringTable follows StrIndexTable
	
		// XMLTags, The XML tag tree starts after some unknown content after the
		// StringTable.  There is some unknown data after the StringTable, scan
		// forward from this point to the flag for the start of an XML start tag.
		int xmlTagOff = LEW(xml, 3*4);  // Start from the offset in the 3rd word.
		// Scan forward until we find the bytes: 0x02011000(x00100102 in normal int)
		for (int ii=xmlTagOff; ii<xml.length-4; ii+=4) {
			if (LEW(xml, ii) == startTag) { 
				xmlTagOff = ii;  break;
			}
		} // end of hack, scanning for start of first start tag
	
		// XML tags and attributes:
		// Every XML start and end tag consists of 6 32 bit words:
		//   0th word: 02011000 for startTag and 03011000 for endTag 
		//   1st word: a flag?, like 38000000
		//   2nd word: Line of where this tag appeared in the original source file
		//   3rd word: FFFFFFFF ??
		//   4th word: StringIndex of NameSpace name, or FFFFFFFF for default NS
		//   5th word: StringIndex of Element Name
		//   (Note: 01011000 in 0th word means end of XML document, endDocTag)
	
		// Start tags (not end tags) contain 3 more words:
		//   6th word: 14001400 meaning?? 
		//   7th word: Number of Attributes that follow this tag(follow word 8th)
		//   8th word: 00000000 meaning??
	
		// Attributes consist of 5 words: 
		//   0th word: StringIndex of Attribute Name's Namespace, or FFFFFFFF
		//   1st word: StringIndex of Attribute Name
		//   2nd word: StringIndex of Attribute Value, or FFFFFFF if ResourceId used
		//   3rd word: Flags?
		//   4th word: str ind of attr value again, or ResourceId of value
	
		// Step through the XML tree element tags and attributes
		String attrValue;
		String attrName;
		int off = xmlTagOff;
		while (off < xml.length) {
			int tag0 = LEW(xml, off);
			if (tag0 == startTag) { // XML START TAG
				int numbAttrs = LEW(xml, off+7*4);  // Number of Attributes to follow
				off += 9*4;  // Skip over 6+3 words of startTag data		
				// Look for the Attributes
				for (int ii=0; ii<numbAttrs; ii++) {
					int attrNameSi = LEW(xml, off+1*4);  // AttrName String Index
					int attrValueSi = LEW(xml, off+2*4); // AttrValue Str Ind, or FFFFFFFF
					int attrResId = LEW(xml, off+4*4);  // AttrValue ResourceId or dup AttrValue StrInd
					off += 5*4;  // Skip over the 5 words of an attribute
		
					attrName = compXmlString(xml, sitOff, stOff, attrNameSi);
					if (attrName.equals("scheme")) {
						attrValue = attrValueSi!=-1 ? compXmlString(xml, sitOff, stOff, attrValueSi) : "resourceID 0x"+Integer.toHexString(attrResId);
						if (validURI(attrValue))
							return attrValue;
					}
				}
		
			} else if (tag0 == endTag) { // XML END TAG
				off += 6*4;  // Skip over 6 words of endTag data
			} else if (tag0 == endDocTag) {  // END OF XML DOC TAG
				break;
			} else {
				break;
			}
		} // end of while loop scanning tags and attributes of XML tree
		
		return SystemObserver.BLANK;
	} // end of decompressXML

    /**
     * <P>Checks whether the supplied {@link String} is of a valid/known URI protocol type.</P>
     *
     * <P>
     *     Valid protocol types:
     * <ul>
     *     <li>http</li>
     *     <li>https</li>
     *     <li>geo</li>
     *     <li>*</li>
     *     <li>package</li>
     *     <li>sms</li>
     *     <li>smsto</li>
     *     <li>mms</li>
     *     <li>mmsto</li>
     *     <li>tel</li>
     *     <li>voicemail</li>
     *     <li>file</li>
     *     <li>content</li>
     *     <li>mailto</li>
     * </ul>
     *
     * </P>
     *
     * @param value The {@link String} value to be assessed.
     * @return A {@link Boolean} value; if valid returns true, else false.
     */
	private boolean validURI(String value) {
		if (value != null) {
			if (!value.equals("http") 
					&& !value.equals("https") 
					&& !value.equals("geo") 
					&& !value.equals("*") 
					&& !value.equals("package") 
					&& !value.equals("sms") 
					&& !value.equals("smsto")
					&& !value.equals("mms") 
					&& !value.equals("mmsto")
					&& !value.equals("tel")
					&& !value.equals("voicemail")
					&& !value.equals("file")
					&& !value.equals("content")
					&& !value.equals("mailto")) {
				return true;
			}
		}
		return false;
	}

    /**
     *
     *
     * @param xml
     * @param sitOff
     * @param stOff
     * @param strInd
     * @return
     */
	public String compXmlString(byte[] xml, int sitOff, int stOff, int strInd) {
		if (strInd < 0) return null;
		int strOff = stOff + LEW(xml, sitOff+strInd*4);
		return compXmlStringAt(xml, strOff);
	}

	// compXmlStringAt -- Return the string stored in StringTable format at
	// offset strOff.  This offset points to the 16 bit string length, which 
	// is followed by that number of 16 bit (Unicode) chars.
	public String compXmlStringAt(byte[] arr, int strOff) {
		int strLen = arr[strOff+1]<<8&0xff00 | arr[strOff]&0xff;
		byte[] chars = new byte[strLen];
		for (int ii=0; ii<strLen; ii++) {
			chars[ii] = arr[strOff+2+ii*2];
		}
		return new String(chars);  // Hack, just use 8 byte chars
	} // end of compXmlStringAt

    /**
     * LEW (Little-Endian Word)
     *
     * @param arr The {@link Byte[]} to process.
     * @param off An {@link int} value indicating the offset from which the return value should be
     *            taken.
     * @return The {@link int} Little Endian 32 bit word taken from the input {@link Byte[]} at the
     * {@link int} offset provided.
     */
	public int LEW(byte[] arr, int off) {
		return arr[off+3]<<24&0xff000000 | arr[off+2]<<16&0xff0000 | arr[off+1]<<8&0xff00 | arr[off]&0xFF;
	} // end of LEW
}
