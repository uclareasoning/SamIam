package il2.bridge;

import java.util.Map;

/** @author Keith Cascio
	@since 20031125 */
public class ConversionException extends RuntimeException
{
	public ConversionException( Converter conv, Object missing )
	{
		myConverter = conv;
		myMissing = missing;
	}

	public Converter getConverter()
	{
		return myConverter;
	}

	public Object getMissing()
	{
		return myMissing;
	}

	public String getMessage()
	{
		String strMissing = (myMissing == null) ? "null" : myMissing.toString();
		String strIndex = "null";
		if( myConverter != null ){
			Map index = myConverter.getIndex();
			if( index != null ) strIndex = index.toString();
		}
		return "conversion failed for Object " + strMissing + ", index: " + strIndex;
	}

	private Converter myConverter;
	private Object myMissing;
}
