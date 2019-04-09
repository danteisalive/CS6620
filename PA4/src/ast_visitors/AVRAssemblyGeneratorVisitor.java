package ast_visitors;

/**
 * CheckTypes
 *
 * This AST visitor traverses a MiniJava Abstract Syntax Tree and checks
 * for a number of type errors.  If a type error is found a SymanticException
 * is thrown
 *
 * CHANGES to make next year (2012)
 *  - make the error messages between *, +, and - consistent <= ??
 *
 * Bring down the symtab code so that it only does get and set Type
 *  for expressions
 */

import ast.node.*;
import ast.visitor.DepthFirstVisitor;
import java.util.*;
import java.io.PrintWriter;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import symtable.SymTable;
import symtable.Type;
import exceptions.InternalException;
import exceptions.SemanticException;
import label.Label;

public class AVRAssemblyGeneratorVisitor extends DepthFirstVisitor
{

   private SymTable mCurrentST;
   private String errors;
   private PrintWriter out;

   public AVRAssemblyGeneratorVisitor(PrintWriter out, SymTable st) {
     if(st==null) {
          throw new InternalException("unexpected null argument");
      }
      this.out = out;
      this.mCurrentST = st;
      this.errors = "";
   }

   //========================= Overriding the visitor interface


     public void defaultOut(Node node) {
         //System.err.println("Node not implemented in CheckTypes, " + node.getClass());
     }

     public void inProgram(Program node){
         BufferedReader br = null;
         try
         {
            br = new BufferedReader(new FileReader("./src/avrH.rtl.s"));
            String line = br.readLine();
            while (line != null) {
                out.println(line);
                line = br.readLine();
            }
            out.println("");
         }
         catch (Exception e1){
            e1.printStackTrace();
         }
         finally {
           try{ if (br != null) br.close();}
           catch(Exception e2) { e2.printStackTrace(); }
         }

     }

     public void outProgram(Program node){
       out.flush();
     }

     public void outMainClass(MainClass node){
       BufferedReader br = null;
       try
       {
          br = new BufferedReader(new FileReader("./src/avrF.rtl.s"));
          String line = br.readLine();
          while (line != null) {
              out.println(line);
              line = br.readLine();
          }
          out.println("");
       }
       catch (Exception e1){
          e1.printStackTrace();
       }
       finally {
         try{ if (br != null) br.close();}
         catch(Exception e2) { e2.printStackTrace(); }
       }
     }


     public void outIntegerExp(IntLiteral node){
       Integer val = node.getIntValue();
       out.println("    # Load constant int " + val);
       out.println("    ldi    r24,lo8(" + val + ")");
       out.println("    ldi    r25,hi8(" + val + ")");
       out.println("    # push two byte expression onto stack");
       out.println("    push   r25");
       out.println("    push   r24");
       out.println("");
     }

     public void outByteCast(ByteCast node){
        if (this.mCurrentST.getExpType(node.getExp()) == Type.BYTE){
          return;
        }
        out.println("    # Casting int to byte by popping");
        out.println("    # 2 bytes off stack and only pushing low order bits");
        out.println("    # back on.  Low order bits are on top of stack.");
        out.println("    pop    r24");
        out.println("    pop    r25");
        out.println("    push   r24");
        out.println("");

     }

     public void outColorExp(ColorLiteral node){
       out.println("    # Color expression " + node.toString());
       out.println("    ldi    r22," + node.getIntValue());
       out.println("    # push one byte expression onto stack");
       out.println("    push   r22");
       out.println("");
     }

     public void outMeggySetPixel(MeggySetPixel node){
       out.println("    ### Meggy.setPixel(x,y,color) call");
       out.println("    # load a one byte expression off stack");
       out.println("    pop    r20");
       out.println("    # load a one byte expression off stack");
       out.println("    pop    r22");
       out.println("    # load a one byte expression off stack");
       out.println("    pop    r24");
       out.println("    call   _Z6DrawPxhhh");
       out.println("    call   _Z12DisplaySlatev");
     }
     public void outPlusExp(PlusExp node){
         String branch_1 = new Label().toString();
         String branch_2 = new Label().toString();
         String branch_3 = new Label().toString();
         String branch_4 = new Label().toString();

         if(this.mCurrentST.getExpType(node.getLExp()) == Type.INT &&
           this.mCurrentST.getExpType(node.getRExp()) == Type.INT){
           out.println("# load a two byte expression off stack");
           out.println("pop    r18");
           out.println("pop    r19");
           out.println("# load a two byte expression off stack");
           out.println("pop    r24");
           out.println("pop    r25");
           out.println("# Do add operation");
           out.println("add    r24, r18");
           out.println("adc    r25, r19");
           out.println("# push two byte expression onto stack");
           out.println("push   r25");
           out.println("push   r24");
           out.println("");
         }

         else if(this.mCurrentST.getExpType(node.getLExp()) == Type.BYTE &&
             this.mCurrentST.getExpType(node.getRExp()) == Type.BYTE){
           out.println("# load a one byte expression off stack");
           out.println("pop    r18");
           out.println("# load a one byte expression off stack");
           out.println("pop    r24");
           out.println("# promoting a byte to an int");
           out.println("tst     r24");
           out.println("brlt     " + branch_1); // branch_1
           out.println("ldi    r25, 0");
           out.println("jmp    " + branch_2); // branch_2
           out.println(branch_1 + ":"); // branch_1
           out.println("ldi    r25, hi8(-1)");
           out.println(branch_2 + ":"); // branch_2
           out.println("# promoting a byte to an int");
           out.println("tst     r18");
           out.println("brlt     " + branch_3); // branch_3
           out.println("ldi    r19, 0");
           out.println("jmp    " + branch_4); // branch_4
           out.println(branch_3 + ":"); // branch_3
           out.println("ldi    r19, hi8(-1)");
           out.println(branch_4 + ":");	// branch_4
           out.println("# Do add operation");
           out.println("add    r24, r18");
           out.println("adc    r25, r19");
           out.println("# push two byte expression onto stack");
           out.println("push   r25");
           out.println("push   r24");
           out.println("");

         }

       else if(this.mCurrentST.getExpType(node.getLExp()) == Type.BYTE &&
           this.mCurrentST.getExpType(node.getRExp()) == Type.INT){
           out.println("# load a two byte expression off stack");
           out.println("pop    r18");
           out.println("pop    r19");
           out.println("# load a one byte expression off stack");
           out.println("pop    r24");
           out.println("# promoting a byte to an int");
           out.println("tst     r24");
           out.println("brlt     " + branch_1); // branch_1
           out.println("ldi    r25, 0");
           out.println("jmp    " + branch_2); // branch_2
           out.println(branch_1 + ":"); // branch_1
           out.println("ldi    r25, hi8(-1)");
           out.println(branch_2 + ":"); // branch_2
           out.println("# Do add operation");
           out.println("add    r24, r18");
           out.println("adc    r25, r19");
           out.println("# push two byte expression onto stack");
           out.println("push   r25");
           out.println("push   r24");
           out.println("");
       }
       else if(this.mCurrentST.getExpType(node.getLExp()) == Type.INT &&
           this.mCurrentST.getExpType(node.getRExp()) == Type.BYTE){
           out.println("# load a one byte expression off stack");
           out.println("pop    r18");
           out.println("# load a two byte expression off stack");
           out.println("pop    r24");
           out.println("pop    r25");
           out.println("# promoting a byte to an int");
           out.println("tst     r18");
           out.println("brlt     " + branch_1); // branch_1
           out.println("ldi    r19, 0");
           out.println("jmp    " + branch_2); // branch_2
           out.println(branch_1 + ":"); // branch_1
           out.println("ldi    r19, hi8(-1)");
           out.println(branch_2 + ":"); // branch_2
           out.println("# Do add operation");
           out.println("add    r24, r18");
           out.println("adc    r25, r19");
           out.println("# push two byte expression onto stack");
           out.println("push   r25");
           out.println("push   r24");
           out.println("");
       }
 }

}
