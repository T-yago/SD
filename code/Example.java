
import sd23.*;

public class Example {
    public static void main(String[] args) {
         
        try {
            // obter a tarefa de ficheiro, socket, etc...
            byte[] job = new byte[1000];

            // executar a tarefa
            byte[] output = JobFunction.execute(job);

            // utilizar o resultado ou reportar o erro
            System.err.println("success, returned "+output.length+" bytes");
        } catch (JobFunctionException e) {
            System.err.println("job failed: code="+e.getCode()+" message="+e.getMessage());
        }
    }
}
