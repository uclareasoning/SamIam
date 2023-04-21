#if(PROFILE)
#include <time.h>

unsigned __int64 GetMachineCycleCount()
{      
	unsigned __int32 cycles_high=0;
	unsigned __int32 cycles_low=0;

   _asm pushad; /* save */
   _asm cpuid; /* serialize */
   _asm rdtsc; /* read clock */
   _asm mov cycles_low, eax /* copy low 32 bits */
   _asm mov cycles_high, edx /* copy high 32 bits */
   _asm popad; /* save */

   /* combine and return low/high bits */
   return ((unsigned __int64)cycles_high << 32) | cycles_low;
}
#endif
