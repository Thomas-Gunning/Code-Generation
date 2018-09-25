

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */


class CodegenException extends Exception {
public String msg;
public CodegenException ( String _msg ) { msg = _msg; } }

interface Codegen {
public String codegen ( Program p ) throws CodegenException; }


class Task3  {
    public static Codegen create () throws CodegenException {
        TheCodeGenerator c = new TheCodeGenerator();
        return c;
    } 
} 