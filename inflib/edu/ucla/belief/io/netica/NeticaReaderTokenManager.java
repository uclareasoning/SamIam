/* Generated By:JavaCC: Do not edit this line. NeticaReaderTokenManager.java */
package edu.ucla.belief.io.netica;
import java.util.*;
import java.io.*;
import edu.ucla.belief.io.*;

public class NeticaReaderTokenManager implements NeticaReaderConstants
{
  public  java.io.PrintStream debugStream = System.out;
  public  void setDebugStream(java.io.PrintStream ds) { debugStream = ds; }
private final int jjStopStringLiteralDfa_0(int pos, long active0)
{
   switch (pos)
   {
      case 0:
         if ((active0 & 0x80L) != 0L)
         {
            jjmatchedKind = 8;
            return 1;
         }
         return -1;
      case 1:
         if ((active0 & 0x80L) != 0L)
         {
            jjmatchedKind = 8;
            jjmatchedPos = 1;
            return 1;
         }
         return -1;
      case 2:
         if ((active0 & 0x80L) != 0L)
         {
            jjmatchedKind = 8;
            jjmatchedPos = 2;
            return 1;
         }
         return -1;
      case 3:
         if ((active0 & 0x80L) != 0L)
         {
            jjmatchedKind = 8;
            jjmatchedPos = 3;
            return 1;
         }
         return -1;
      case 4:
         if ((active0 & 0x80L) != 0L)
         {
            jjmatchedKind = 8;
            jjmatchedPos = 4;
            return 1;
         }
         return -1;
      default :
         return -1;
   }
}
private final int jjStartNfa_0(int pos, long active0)
{
   return jjMoveNfa_0(jjStopStringLiteralDfa_0(pos, active0), pos + 1);
}
private final int jjStopAtPos(int pos, int kind)
{
   jjmatchedKind = kind;
   jjmatchedPos = pos;
   return pos + 1;
}
private final int jjStartNfaWithStates_0(int pos, int kind, int state)
{
   jjmatchedKind = kind;
   jjmatchedPos = pos;
   try { curChar = input_stream.readChar(); }
   catch(java.io.IOException e) { return pos + 1; }
   return jjMoveNfa_0(state, pos + 1);
}
private final int jjMoveStringLiteralDfa0_0()
{
   switch(curChar)
   {
      case 40:
         return jjStopAtPos(0, 15);
      case 41:
         return jjStopAtPos(0, 17);
      case 44:
         return jjStopAtPos(0, 16);
      case 59:
         return jjStopAtPos(0, 11);
      case 61:
         return jjStopAtPos(0, 14);
      case 64:
         return jjMoveStringLiteralDfa1_0(0x40L);
      case 100:
         return jjMoveStringLiteralDfa1_0(0x80L);
      case 123:
         return jjStopAtPos(0, 12);
      case 125:
         return jjStopAtPos(0, 13);
      default :
         return jjMoveNfa_0(0, 0);
   }
}
private final int jjMoveStringLiteralDfa1_0(long active0)
{
   try { curChar = input_stream.readChar(); }
   catch(java.io.IOException e) {
      jjStopStringLiteralDfa_0(0, active0);
      return 1;
   }
   switch(curChar)
   {
      case 101:
         return jjMoveStringLiteralDfa2_0(active0, 0x80L);
      case 117:
         return jjMoveStringLiteralDfa2_0(active0, 0x40L);
      default :
         break;
   }
   return jjStartNfa_0(0, active0);
}
private final int jjMoveStringLiteralDfa2_0(long old0, long active0)
{
   if (((active0 &= old0)) == 0L)
      return jjStartNfa_0(0, old0);
   try { curChar = input_stream.readChar(); }
   catch(java.io.IOException e) {
      jjStopStringLiteralDfa_0(1, active0);
      return 2;
   }
   switch(curChar)
   {
      case 102:
         return jjMoveStringLiteralDfa3_0(active0, 0x80L);
      case 110:
         return jjMoveStringLiteralDfa3_0(active0, 0x40L);
      default :
         break;
   }
   return jjStartNfa_0(1, active0);
}
private final int jjMoveStringLiteralDfa3_0(long old0, long active0)
{
   if (((active0 &= old0)) == 0L)
      return jjStartNfa_0(1, old0);
   try { curChar = input_stream.readChar(); }
   catch(java.io.IOException e) {
      jjStopStringLiteralDfa_0(2, active0);
      return 3;
   }
   switch(curChar)
   {
      case 100:
         return jjMoveStringLiteralDfa4_0(active0, 0x40L);
      case 105:
         return jjMoveStringLiteralDfa4_0(active0, 0x80L);
      default :
         break;
   }
   return jjStartNfa_0(2, active0);
}
private final int jjMoveStringLiteralDfa4_0(long old0, long active0)
{
   if (((active0 &= old0)) == 0L)
      return jjStartNfa_0(2, old0);
   try { curChar = input_stream.readChar(); }
   catch(java.io.IOException e) {
      jjStopStringLiteralDfa_0(3, active0);
      return 4;
   }
   switch(curChar)
   {
      case 101:
         return jjMoveStringLiteralDfa5_0(active0, 0x40L);
      case 110:
         return jjMoveStringLiteralDfa5_0(active0, 0x80L);
      default :
         break;
   }
   return jjStartNfa_0(3, active0);
}
private final int jjMoveStringLiteralDfa5_0(long old0, long active0)
{
   if (((active0 &= old0)) == 0L)
      return jjStartNfa_0(3, old0);
   try { curChar = input_stream.readChar(); }
   catch(java.io.IOException e) {
      jjStopStringLiteralDfa_0(4, active0);
      return 5;
   }
   switch(curChar)
   {
      case 101:
         if ((active0 & 0x80L) != 0L)
            return jjStartNfaWithStates_0(5, 7, 1);
         break;
      case 102:
         if ((active0 & 0x40L) != 0L)
            return jjStopAtPos(5, 6);
         break;
      default :
         break;
   }
   return jjStartNfa_0(4, active0);
}
private final void jjCheckNAdd(int state)
{
   if (jjrounds[state] != jjround)
   {
      jjstateSet[jjnewStateCnt++] = state;
      jjrounds[state] = jjround;
   }
}
private final void jjAddStates(int start, int end)
{
   do {
      jjstateSet[jjnewStateCnt++] = jjnextStates[start];
   } while (start++ != end);
}
private final void jjCheckNAddTwoStates(int state1, int state2)
{
   jjCheckNAdd(state1);
   jjCheckNAdd(state2);
}
private final void jjCheckNAddStates(int start, int end)
{
   do {
      jjCheckNAdd(jjnextStates[start]);
   } while (start++ != end);
}
private final void jjCheckNAddStates(int start)
{
   jjCheckNAdd(jjnextStates[start]);
   jjCheckNAdd(jjnextStates[start + 1]);
}
static final long[] jjbitVec0 = {
   0x0L, 0x0L, 0xffffffffffffffffL, 0xffffffffffffffffL
};
private final int jjMoveNfa_0(int startState, int curPos)
{
   int[] nextStates;
   int startsAt = 0;
   jjnewStateCnt = 37;
   int i = 1;
   jjstateSet[0] = startState;
   int j, kind = 0x7fffffff;
   for (;;)
   {
      if (++jjround == 0x7fffffff)
         ReInitRounds();
      if (curChar < 64)
      {
         long l = 1L << curChar;
         MatchLoop: do
         {
            switch(jjstateSet[--i])
            {
               case 0:
                  if ((0x3ff000000000000L & l) != 0L)
                  {
                     if (kind > 9)
                        kind = 9;
                     jjCheckNAddStates(0, 3);
                  }
                  else if ((0x280000000000L & l) != 0L)
                     jjCheckNAddTwoStates(3, 8);
                  else if (curChar == 47)
                     jjAddStates(4, 5);
                  else if (curChar == 34)
                     jjCheckNAddStates(6, 8);
                  else if (curChar == 46)
                     jjCheckNAdd(4);
                  if (curChar == 45)
                     jjstateSet[jjnewStateCnt++] = 19;
                  break;
               case 1:
                  if ((0x3ff000000000000L & l) == 0L)
                     break;
                  if (kind > 8)
                     kind = 8;
                  jjstateSet[jjnewStateCnt++] = 1;
                  break;
               case 2:
                  if ((0x280000000000L & l) != 0L)
                     jjCheckNAddTwoStates(3, 8);
                  break;
               case 3:
                  if (curChar == 46)
                     jjCheckNAdd(4);
                  break;
               case 4:
                  if ((0x3ff000000000000L & l) == 0L)
                     break;
                  if (kind > 9)
                     kind = 9;
                  jjCheckNAddTwoStates(4, 5);
                  break;
               case 6:
                  if ((0x280000000000L & l) != 0L)
                     jjCheckNAdd(7);
                  break;
               case 7:
                  if ((0x3ff000000000000L & l) == 0L)
                     break;
                  if (kind > 9)
                     kind = 9;
                  jjCheckNAdd(7);
                  break;
               case 8:
                  if ((0x3ff000000000000L & l) == 0L)
                     break;
                  if (kind > 9)
                     kind = 9;
                  jjCheckNAddStates(0, 3);
                  break;
               case 9:
                  if ((0x3ff000000000000L & l) == 0L)
                     break;
                  if (kind > 9)
                     kind = 9;
                  jjCheckNAddTwoStates(9, 5);
                  break;
               case 10:
                  if ((0x3ff000000000000L & l) != 0L)
                     jjCheckNAddTwoStates(10, 3);
                  break;
               case 11:
                  if (curChar == 45)
                     jjstateSet[jjnewStateCnt++] = 19;
                  break;
               case 20:
                  if (curChar == 34)
                     jjCheckNAddStates(6, 8);
                  break;
               case 21:
                  if ((0xfffffffbffffffffL & l) != 0L)
                     jjCheckNAddStates(6, 8);
                  break;
               case 23:
                  jjCheckNAddStates(6, 8);
                  break;
               case 24:
                  if (curChar == 34 && kind > 10)
                     kind = 10;
                  break;
               case 25:
                  if (curChar == 47)
                     jjAddStates(4, 5);
                  break;
               case 26:
                  if (curChar == 47)
                     jjCheckNAddStates(9, 11);
                  break;
               case 27:
                  if ((0xffffffffffffdbffL & l) != 0L)
                     jjCheckNAddStates(9, 11);
                  break;
               case 28:
                  if ((0x2400L & l) != 0L && kind > 5)
                     kind = 5;
                  break;
               case 29:
                  if (curChar == 10 && kind > 5)
                     kind = 5;
                  break;
               case 30:
                  if (curChar == 13)
                     jjstateSet[jjnewStateCnt++] = 29;
                  break;
               case 31:
                  if (curChar == 42)
                     jjCheckNAddStates(12, 14);
                  break;
               case 32:
                  if ((0xfffffbffffffffffL & l) != 0L)
                     jjCheckNAddStates(12, 14);
                  break;
               case 33:
                  if (curChar == 42)
                     jjstateSet[jjnewStateCnt++] = 34;
                  break;
               case 34:
                  if ((0xffff7fffffffffffL & l) != 0L)
                     jjCheckNAddStates(12, 14);
                  break;
               case 35:
                  if (curChar == 47 && kind > 5)
                     kind = 5;
                  break;
               case 36:
                  if (curChar == 42)
                     jjstateSet[jjnewStateCnt++] = 35;
                  break;
               default : break;
            }
         } while(i != startsAt);
      }
      else if (curChar < 128)
      {
         long l = 1L << (curChar & 077);
         MatchLoop: do
         {
            switch(jjstateSet[--i])
            {
               case 0:
                  if ((0x7fffffe07fffffeL & l) != 0L)
                  {
                     if (kind > 8)
                        kind = 8;
                     jjCheckNAdd(1);
                  }
                  if (curChar == 73)
                     jjstateSet[jjnewStateCnt++] = 18;
                  break;
               case 1:
                  if ((0x7fffffe87fffffeL & l) == 0L)
                     break;
                  if (kind > 8)
                     kind = 8;
                  jjCheckNAdd(1);
                  break;
               case 5:
                  if ((0x2000000020L & l) != 0L)
                     jjAddStates(15, 16);
                  break;
               case 12:
                  if (curChar == 89 && kind > 9)
                     kind = 9;
                  break;
               case 13:
                  if (curChar == 84)
                     jjstateSet[jjnewStateCnt++] = 12;
                  break;
               case 14:
                  if (curChar == 73)
                     jjstateSet[jjnewStateCnt++] = 13;
                  break;
               case 15:
                  if (curChar == 78)
                     jjstateSet[jjnewStateCnt++] = 14;
                  break;
               case 16:
                  if (curChar == 73)
                     jjstateSet[jjnewStateCnt++] = 15;
                  break;
               case 17:
                  if (curChar == 70)
                     jjstateSet[jjnewStateCnt++] = 16;
                  break;
               case 18:
                  if (curChar == 78)
                     jjstateSet[jjnewStateCnt++] = 17;
                  break;
               case 19:
                  if (curChar == 73)
                     jjstateSet[jjnewStateCnt++] = 18;
                  break;
               case 21:
                  if ((0xffffffffefffffffL & l) != 0L)
                     jjCheckNAddStates(6, 8);
                  break;
               case 22:
                  if (curChar == 92)
                     jjstateSet[jjnewStateCnt++] = 23;
                  break;
               case 23:
                  if ((0xfffffbffffffffffL & l) != 0L)
                     jjCheckNAddStates(6, 8);
                  break;
               case 27:
                  jjAddStates(9, 11);
                  break;
               case 32:
               case 34:
                  jjCheckNAddStates(12, 14);
                  break;
               default : break;
            }
         } while(i != startsAt);
      }
      else
      {
         int i2 = (curChar & 0xff) >> 6;
         long l2 = 1L << (curChar & 077);
         MatchLoop: do
         {
            switch(jjstateSet[--i])
            {
               case 21:
               case 23:
                  if ((jjbitVec0[i2] & l2) != 0L)
                     jjCheckNAddStates(6, 8);
                  break;
               case 27:
                  if ((jjbitVec0[i2] & l2) != 0L)
                     jjAddStates(9, 11);
                  break;
               case 32:
               case 34:
                  if ((jjbitVec0[i2] & l2) != 0L)
                     jjCheckNAddStates(12, 14);
                  break;
               default : break;
            }
         } while(i != startsAt);
      }
      if (kind != 0x7fffffff)
      {
         jjmatchedKind = kind;
         jjmatchedPos = curPos;
         kind = 0x7fffffff;
      }
      ++curPos;
      if ((i = jjnewStateCnt) == (startsAt = 37 - (jjnewStateCnt = startsAt)))
         return curPos;
      try { curChar = input_stream.readChar(); }
      catch(java.io.IOException e) { return curPos; }
   }
}
static final int[] jjnextStates = {
   9, 10, 3, 5, 26, 31, 21, 22, 24, 27, 28, 30, 32, 33, 36, 6,
   7,
};
public static final String[] jjstrLiteralImages = {
"", null, null, null, null, null, "\100\165\156\144\145\146",
"\144\145\146\151\156\145", null, null, null, "\73", "\173", "\175", "\75", "\50", "\54", "\51", };
public static final String[] lexStateNames = {
   "DEFAULT",
};
static final long[] jjtoToken = {
   0x3ffc1L,
};
static final long[] jjtoSkip = {
   0x3eL,
};
static final long[] jjtoSpecial = {
   0x20L,
};
private SimpleCharStream input_stream;
private final int[] jjrounds = new int[37];
private final int[] jjstateSet = new int[74];
protected char curChar;
public NeticaReaderTokenManager(SimpleCharStream stream)
{
   if (SimpleCharStream.staticFlag)
      throw new Error("ERROR: Cannot use a static CharStream class with a non-static lexical analyzer.");
   input_stream = stream;
}
public NeticaReaderTokenManager(SimpleCharStream stream, int lexState)
{
   this(stream);
   SwitchTo(lexState);
}
public void ReInit(SimpleCharStream stream)
{
   jjmatchedPos = jjnewStateCnt = 0;
   curLexState = defaultLexState;
   input_stream = stream;
   ReInitRounds();
}
private final void ReInitRounds()
{
   int i;
   jjround = 0x80000001;
   for (i = 37; i-- > 0;)
      jjrounds[i] = 0x80000000;
}
public void ReInit(SimpleCharStream stream, int lexState)
{
   ReInit(stream);
   SwitchTo(lexState);
}
public void SwitchTo(int lexState)
{
   if (lexState >= 1 || lexState < 0)
      throw new TokenMgrError("Error: Ignoring invalid lexical state : " + lexState + ". State unchanged.", TokenMgrError.INVALID_LEXICAL_STATE);
   else
      curLexState = lexState;
}

private final Token jjFillToken()
{
   Token t = Token.newToken(jjmatchedKind);
   t.kind = jjmatchedKind;
   String im = jjstrLiteralImages[jjmatchedKind];
   t.image = (im == null) ? input_stream.GetImage() : im;
   t.beginLine = input_stream.getBeginLine();
   t.beginColumn = input_stream.getBeginColumn();
   t.endLine = input_stream.getEndLine();
   t.endColumn = input_stream.getEndColumn();
   return t;
}

int curLexState = 0;
int defaultLexState = 0;
int jjnewStateCnt;
int jjround;
int jjmatchedPos;
int jjmatchedKind;

public final Token getNextToken()
{
  int kind;
  Token specialToken = null;
  Token matchedToken;
  int curPos = 0;

  EOFLoop :
  for (;;)
  {
   try
   {
      curChar = input_stream.BeginToken();
   }
   catch(java.io.IOException e)
   {
      jjmatchedKind = 0;
      matchedToken = jjFillToken();
      matchedToken.specialToken = specialToken;
      return matchedToken;
   }

   try { input_stream.backup(0);
      while (curChar <= 32 && (0x100002600L & (1L << curChar)) != 0L)
         curChar = input_stream.BeginToken();
   }
   catch (java.io.IOException e1) { continue EOFLoop; }
   jjmatchedKind = 0x7fffffff;
   jjmatchedPos = 0;
   curPos = jjMoveStringLiteralDfa0_0();
   if (jjmatchedKind != 0x7fffffff)
   {
      if (jjmatchedPos + 1 < curPos)
         input_stream.backup(curPos - jjmatchedPos - 1);
      if ((jjtoToken[jjmatchedKind >> 6] & (1L << (jjmatchedKind & 077))) != 0L)
      {
         matchedToken = jjFillToken();
         matchedToken.specialToken = specialToken;
         return matchedToken;
      }
      else
      {
         if ((jjtoSpecial[jjmatchedKind >> 6] & (1L << (jjmatchedKind & 077))) != 0L)
         {
            matchedToken = jjFillToken();
            if (specialToken == null)
               specialToken = matchedToken;
            else
            {
               matchedToken.specialToken = specialToken;
               specialToken = (specialToken.next = matchedToken);
            }
         }
         continue EOFLoop;
      }
   }
   int error_line = input_stream.getEndLine();
   int error_column = input_stream.getEndColumn();
   String error_after = null;
   boolean EOFSeen = false;
   try { input_stream.readChar(); input_stream.backup(1); }
   catch (java.io.IOException e1) {
      EOFSeen = true;
      error_after = curPos <= 1 ? "" : input_stream.GetImage();
      if (curChar == '\n' || curChar == '\r') {
         error_line++;
         error_column = 0;
      }
      else
         error_column++;
   }
   if (!EOFSeen) {
      input_stream.backup(1);
      error_after = curPos <= 1 ? "" : input_stream.GetImage();
   }
   throw new TokenMgrError(EOFSeen, curLexState, error_line, error_column, error_after, curChar, TokenMgrError.LEXICAL_ERROR);
  }
}

}