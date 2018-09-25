

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */


import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
/**
 *
 * 
 */
public class TheCodeGenerator implements Codegen {
    Program p;
    Stack breaks = new Stack();
    Stack continues = new Stack();
    List<Declaration> decs = new ArrayList<>();
    String mips = ""; 
    int count = 0;
    
    @Override
    
    //this the codegen it takes in program p, loops through all the
    //declarations and gets there body
    public String codegen(Program p) throws CodegenException {
       mips = "";
       this.p = p;
       List<Declaration> pDecls = p.decls;
       mips += "entry_point: \n";
       mips += "jal " + p.decls.get(0).id + "_entry \n";
       mips += "li $v0 10 \n";
       mips += "syscall \n";
       for(Declaration dec: pDecls){
           genDecl(dec);
       }
       
       return mips;
   
    
    
    }
    
    public void genDecl(Declaration dec){
        int sizeAR = (2 + dec.numOfArgs) * 4;
        mips += dec.id + "_entry: \n";
        mips += "move $fp $sp \n";
        mips += "sw $ra 0($sp) \n";
        mips += "addiu $sp $sp -4 \n";
        genExp(dec.body);
        mips += "lw $ra 4($sp) \n";
        mips += "addiu $sp $sp " + sizeAR + "\n";
        mips += "lw $fp 0($sp) \n";
        mips += "jr $ra \n";
        
    }
    //this generates the expression for each mips command
    //It checks all the possible AST 
    public void genExp(Exp body){
        //checks if intliteral then produces the mips command 
        if(body instanceof IntLiteral){
            mips += "li $a0 " + ((IntLiteral)body).n + "\n";
            //System.out.println(mips); 
        }
        //checks if Vairable then produces the mips command 
        if(body instanceof Variable )
        {
            int offset = 4 * ((Variable)body).x;
            mips += "lw $a0 " + offset + "($fp) \n";
        }
        //checks if is an instance of the BinExp 
        if(body instanceof Binexp){
            //if it is plus call gen plus
            if(((Binexp)body).binop instanceof Plus){
                genPlus(body);
            }
            //if it is minus call gen minus
            if(((Binexp)body).binop instanceof Minus){
                genMinus(body);
            }
            if(((Binexp)body).binop instanceof Div){
                genDiv(body);
            }
             if(((Binexp)body).binop instanceof Times){
                genMult(body);
            }
        }
        //conditional 
        if(body instanceof If){
            String elseBranch = labelGen();
            String thenBranch = labelGen();
            String exitLabel = labelGen();
            genExp(((If)body).l);
            mips += "sw $a0 0($sp) \n";
            mips += "addiu $sp $sp-4 \n";
            genExp(((If)body).r);
            mips += "lw $t1 4($sp) \n";
            mips += "addiu $sp $sp 4 \n ";
            
            
           if(((If)body).comp instanceof Equals){   
            mips += "beq $t1 $a0 " + thenBranch + "\n";
            
            mips += elseBranch + ": \n";
            genExp(((If)body).elseBody);
            mips += "b " + exitLabel + "\n"; 
            mips += thenBranch + ": \n";
            genExp(((If)body).thenBody);
            mips += exitLabel + ": \n";  
           }
           else if(((If)body).comp instanceof Less){
            mips += "blt $t1 $a0 " + thenBranch + "\n";
            
            mips += elseBranch + ": \n";
            genExp(((If)body).elseBody);
            mips += "b " + exitLabel + "\n"; 
            mips += thenBranch + ": \n";
            genExp(((If)body).thenBody);
            mips += exitLabel + ": \n";  
           }
           else if(((If)body).comp instanceof Greater){
            mips += "bgt $t1 $a0  " + thenBranch + "\n";
           
            mips += elseBranch + ": \n";
            genExp(((If)body).elseBody);
            mips += "b " + exitLabel + "\n"; 
            mips += thenBranch + ": \n";
            genExp(((If)body).thenBody);
            mips += exitLabel + ": \n";  
           }
           else if(((If)body).comp instanceof LessEq){
            mips += "ble $t1 $a0  " + thenBranch + "\n";
            
            mips += elseBranch + ": \n";
            genExp(((If)body).elseBody);
            mips += "b " + exitLabel + "\n"; 
            mips += thenBranch + ": \n";
            genExp(((If)body).thenBody);
            mips += exitLabel + ": \n";  
           }
            else if(((If)body).comp instanceof GreaterEq){
            mips += "bge $t1 $a0  " + thenBranch + "\n";
          
            mips += elseBranch + ": \n";
            genExp(((If)body).elseBody);
            mips += "b " + exitLabel + "\n"; 
            mips += thenBranch + ": \n";
            genExp(((If)body).thenBody);
            mips += exitLabel + ": \n";  
           }
          
           
        }
        //This is invoke this allows us to look through all the arguments
        //it loops through the arguments and generates the expressions
        if(body instanceof Invoke){
            mips += "sw $fp 0($sp) \n";
            mips += "addiu $sp $sp -4 \n";
            for(int i = ((Invoke)body).args.size(); i > 0; i--){
               
                genExp(((Invoke)body).args.get(i-1));
                mips += "sw $a0 0($sp) \n";
                mips += "addiu $sp $sp -4 \n";
             }
//            for(Exp b: (((Invoke)body).args)){
//                genExp(b);
//                mips += "sw $a0 0($sp) \n";
//                mips += "addiu $sp $sp -4 \b";
//            }
            mips+= "jal " + (((Invoke)body).name) + "_entry \n";
        }
        if(body instanceof While){
            
            String checkLabel = labelGen();
            String thenBranch = labelGen();
            String exitLabel = labelGen();
            breaks.push(exitLabel);
            continues.push(checkLabel);
            mips += "b " + checkLabel + " \n";
            mips += checkLabel + ": \n";
            genExp(((While)body).l);
            mips += "sw $a0 0($sp) \n";
            mips += "addiu $sp $sp -4 \n";
            genExp(((While)body).r);
            mips += "lw $t1 4($sp) \n";
            mips += "addiu $sp $sp 4 \n";
            if(((While)body).comp instanceof Equals){
                
                 mips += "beq $t1 $a0 " + thenBranch + "\n";
            }
            if(((While)body).comp instanceof Less){
                
                 mips += "blt $t1 $a0 " + thenBranch + "\n";
            }
            if(((While)body).comp instanceof Greater){
                
                 mips += "bgt $t1 $a0 " + thenBranch + "\n";
            }
            if(((While)body).comp instanceof LessEq){
                
                 mips += "ble $t1 $a0 " + thenBranch + "\n";
            }
            if(((While)body).comp instanceof GreaterEq){
                
                 mips += "bge $t1 $a0 " + thenBranch + "\n";
            }
            //this is the exit label 
            mips += "b " + exitLabel + " \n";
            mips += thenBranch + ": \n";
            genExp((((While)body).body));
            mips += "b " + checkLabel + " \n";
            mips += exitLabel + ": \n";
        }
        if(body instanceof RepeatUntil){
            String checkLabel = labelGen();
            String thenBranch = labelGen();
            String exitLabel = labelGen();
            breaks.push(exitLabel);
            continues.push(checkLabel);
            mips += "b " + checkLabel + " \n";
            mips += checkLabel + ": \n";
             genExp(((RepeatUntil)body).l);
            mips += "sw $a0 0($sp) \n";
            mips += "addiu $sp $sp-4 \n";
            genExp(((RepeatUntil)body).r);
            mips += "lw $t1 4($sp) \n";
            mips += "addiu $sp $sp 4 \n ";
            genExp((((RepeatUntil)body).body));
           
            
            
            if(((RepeatUntil)body).comp instanceof Equals){
                
                 mips += "beq $t1 $a0 " + thenBranch + "\n";
            }
            if(((RepeatUntil)body).comp instanceof Less){
                
                 mips += "blt $t1 $a0 " + thenBranch + "\n";
            }
            if(((RepeatUntil)body).comp instanceof Greater){
                
                 mips += "bgt $t1 $a0 " + thenBranch + "\n";
            }
            if(((RepeatUntil)body).comp instanceof LessEq){
                
                 mips += "ble $t1 $a0 " + thenBranch + "\n";
            }
            if(((RepeatUntil)body).comp instanceof GreaterEq){
                
                 mips += "bge $t1 $a0 " + thenBranch + "\n";
            }
            //this is the exit label 
            mips += "b " + exitLabel + " \n";
            mips += exitLabel + ": \n";
            
            mips += thenBranch + ": \n";
            mips += "b " + checkLabel + " \n";
        }
        if(body instanceof Assign){
            int offset = 4 * ((Assign)body).x;
            genExp(((Assign)body).e);
            mips += "sw $a0 " + offset + "($fp) \n";
        } 
        if(body instanceof Seq){
            genExp(((Seq)body).l);
            genExp(((Seq)body).r);
            
        }
        if(body instanceof Skip){
            mips += "li $a0 1\n";
        }
        if(body instanceof Break){
            mips += "b " + breaks.pop() + " \n";
        }
        if(body instanceof Continue){
            mips += "b " + continues.pop() + " \n";
        }
        
      
    }
    //generates a label
    public String labelGen(){
        count++;
        return "label" + count;
    }
    //creates the mips commands to generate a plus 
    public void genPlus(Exp body){
        genExp(((Binexp)body).l);
        mips += "sw $a0 0($sp) \n";
        mips += "addiu $sp $sp -4 \n";
        genExp(((Binexp)body).r);
        mips += "lw $t1 4($sp) \n";
        mips += "add $a0 $t1 $a0 \n";
        mips += "addiu $sp $sp 4\n";
       
    }
    //creates the mips command to generate a minus 
    public void genMinus(Exp body){
        genExp(((Binexp)body).l);
        mips += "sw $a0 0($sp) \n";
        mips += "addiu $sp $sp -4 \n";
        genExp(((Binexp)body).r);
        mips += "lw $t1 4($sp) \n";
        mips += "sub $a0 $t1 $a0 \n";
        mips += "addiu $sp $sp 4\n";
    }
    public void genDiv(Exp body){
        genExp(((Binexp)body).l);
        mips += "sw $a0 0($sp) \n";
        mips += "addiu $sp $sp -4 \n";
        genExp(((Binexp)body).r);
        mips += "lw $t1 4($sp) \n";
        mips += "div $t1 $a0 \n";
        mips += "mflo $a0 \n";
        mips += "addiu $sp $sp 4 \n";
    }
    public void genMult(Exp body){
        genExp(((Binexp)body).l);
        mips += "sw $a0 0($sp) \n";
        mips += "addiu $sp $sp -4 \n";
        genExp(((Binexp)body).r);
        mips += "lw $t1 4($sp) \n";
        mips += "mult $a0 $t1 \n";
        mips += "mflo $a0 \n";
        mips += "addiu $sp $sp 4 \n";
    }

    

   
}
