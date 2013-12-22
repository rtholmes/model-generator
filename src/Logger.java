public class Logger
{
	private int flag;
	private int accessTimeFlag ;
	private int cacheHitFlag;
	
	Logger()
	{
		flag = 1;
		accessTimeFlag = 1;
		cacheHitFlag = 1;
	}
	
	public void disableAll()
	{
		flag = 0;
	}
	
	
	public void disableAccessTimes()
	{
		accessTimeFlag = 0;
	}
	
	public void disableCacheHit()
	{
		cacheHitFlag = 0;
	}
	
	public void enable()
	{
		flag = 1;
		accessTimeFlag = 1;
	}
	
	public void printString(String s)
	{
		if(flag == 1)
			System.out.println(s);
	}
	
	public void printAccessTime(String methodName, String meta, long end, long start)
	{
		if(flag == 1 && accessTimeFlag == 1)
			System.out.println(methodName + " - " + meta + " : " + String.valueOf((double)(end-start)/(1000000000)));
	}
	
	public void printIfCacheHit(String s)
	{
		if(flag == 1 && cacheHitFlag == 1)
			System.out.println(s);
	}
	
}