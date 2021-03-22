public class JDIExampleDebuggee {
    public static void main(String[] args) {
        String jpda = "Java Platform Debugger Architecture";
        System.out.println("Hi Everyone, Welcome to " + jpda); // add a break point here

        String jdi = "Java Debug Interface"; // add a break point here and also stepping in here
        String text = "Today, we'll dive into " + jdi;
        System.out.println(text);


        int i=0;
        int ans = 0;
        for(i=1; i<5; i++)
        {
            ans += i;
        }
        System.out.println(ans);
    }
}