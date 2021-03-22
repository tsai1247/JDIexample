import java.awt.event.KeyListener;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JFrame;
import javax.swing.JRootPane;
import java.awt.event.KeyEvent;

import com.sun.jdi.VirtualMachine;
import com.sun.jdi.Bootstrap;
import com.sun.jdi.IncompatibleThreadStateException;
import com.sun.jdi.AbsentInformationException;
import com.sun.jdi.VMDisconnectedException;
import com.sun.jdi.ClassType;
import com.sun.jdi.StackFrame;
import com.sun.jdi.LocalVariable;
import com.sun.jdi.Value;
import com.sun.jdi.Location;

import com.sun.jdi.connect.Connector;
import com.sun.jdi.connect.LaunchingConnector;

import com.sun.jdi.request.StepRequest;
import com.sun.jdi.request.BreakpointRequest;
import com.sun.jdi.request.ClassPrepareRequest;

import com.sun.jdi.event.ClassPrepareEvent;
import com.sun.jdi.event.LocatableEvent;
import com.sun.jdi.event.EventSet;
import com.sun.jdi.event.Event;
import com.sun.jdi.event.BreakpointEvent;
import com.sun.jdi.event.StepEvent;

public class JDIExampleDebugger {
    private static Logger logger = Logger.getLogger("LoggingDemo");
    private Class debugClass;
    private int[] breakPointLines;
    private Vector<Integer> newBreakPointLines = new Vector<Integer>();
    
    public VirtualMachine connectAndLaunchVM() throws Exception {
 
        LaunchingConnector launchingConnector = Bootstrap.virtualMachineManager()
          .defaultConnector();
        Map<String, Connector.Argument> arguments = launchingConnector.defaultArguments();
        arguments.get("main").setValue(debugClass.getName());
        return (VirtualMachine) launchingConnector.launch(arguments);
    }

    public static void main(String[] args) throws Exception {
        JDIExampleDebugger debuggerInstance = new JDIExampleDebugger();
        debuggerInstance.setDebugClass(JDIExampleDebuggee.class);
        int[] breakPointLines = {1, 2};
        debuggerInstance.setBreakPointLines(breakPointLines);
        VirtualMachine vm = null;
        try {
            vm = debuggerInstance.connectAndLaunchVM();
            debuggerInstance.enableClassPrepareRequest(vm);
            EventSet eventSet = null;
            while ((eventSet = vm.eventQueue().remove()) != null) {
                for (Event event : eventSet) {
                    if (event instanceof ClassPrepareEvent) {
                        debuggerInstance.setBreakPoints(vm, (ClassPrepareEvent)event);
                    }
                    if (event instanceof BreakpointEvent) {
                        debuggerInstance.enableStepRequest(vm, (BreakpointEvent)event);
                    }
                    if (event instanceof StepEvent) {
                        debuggerInstance.displayVariables((StepEvent) event);
                    }
                    vm.resume();
                }
            }
        } catch (VMDisconnectedException e) {
            logger.config("Virtual Machine is disconnected.");
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            System.out.println("--------running-------");
            InputStreamReader reader = new InputStreamReader(vm.process().getInputStream());
            OutputStreamWriter writer = new OutputStreamWriter(System.out);
            char[] buf = new char[512];
            reader.read(buf);
            writer.write(buf);
            writer.flush();
        }
    }

    public void enableStepRequest(VirtualMachine vm, BreakpointEvent event) {
        // enable step request for last break point
        if (event.location().toString().
            contains(debugClass.getName() + ":" + newBreakPointLines.get(newBreakPointLines.size()-1) )) {
            StepRequest stepRequest = vm.eventRequestManager()
                .createStepRequest(event.thread(), StepRequest.STEP_LINE, StepRequest.STEP_OVER);
            stepRequest.enable();    
        }
    }

    //gettter and setter
    public void setBreakPointLines(int[] breakPointLines) {
        this.breakPointLines = breakPointLines;
    }

    private void setDebugClass(Class<JDIExampleDebuggee> debugClass) {
        this.debugClass = debugClass;
    }
    
    public void enableClassPrepareRequest(VirtualMachine vm) {
        ClassPrepareRequest classPrepareRequest = vm.eventRequestManager().createClassPrepareRequest();
        classPrepareRequest.addClassFilter(debugClass.getName());
        classPrepareRequest.enable();
    }

    // TODO: setBreakPoints
    public void setBreakPoints(VirtualMachine vm, ClassPrepareEvent event) throws AbsentInformationException {
        ClassType classType = (ClassType) event.referenceType();
        for( Location location : classType.allLineLocations())
        {
            if(classType.locationsOfLine(location.lineNumber()).get(0).method().toString().equals("JDIExampleDebuggee.main(java.lang.String[])") )
            {
                newBreakPointLines.add(location.lineNumber());
                break;
            }
        }
        // newBreakPointLines.add(classType.allLineLocations().get(0).lineNumber());
        // newBreakPointLines.add(6);
        // newBreakPointLines.add(9);
        
        for(int lineNumber: newBreakPointLines) {
            Location location = classType.locationsOfLine(lineNumber).get(0);
            BreakpointRequest bpReq = vm.eventRequestManager().createBreakpointRequest(location);
            bpReq.enable();
        }
    }

    // TODO: here is what each step should do
    public void displayVariables(LocatableEvent event) throws IncompatibleThreadStateException, AbsentInformationException {
        StackFrame stackFrame = event.thread().frame(0);
        if(stackFrame.location().toString().contains(debugClass.getName())) {
            Map<LocalVariable, Value> visibleVariables = stackFrame
            .getValues(stackFrame.visibleVariables());
            System.out.println("Variables at " + stackFrame.location().toString() +  " > ");
            for (Map.Entry<LocalVariable, Value> entry : visibleVariables.entrySet()) {
                System.out.println(entry.getKey().name() + " = " + entry.getValue() );
            }
            System.out.println();
            getCh();
        }
    }
    public static void getCh() {
        final JFrame frame = new JFrame();
        synchronized (frame) {  
            frame.setUndecorated(true);  
            frame.getRootPane().setWindowDecorationStyle(JRootPane.FRAME);  
            frame.addKeyListener(new KeyListener() {
                @Override 
                public void keyPressed(KeyEvent e) {  
                    synchronized (frame) {  
                        frame.setVisible(false);  
                        frame.dispose();  
                        frame.notify();  
                    }  
                }  
                @Override 
                public void keyReleased(KeyEvent e) {  
                }  
                @Override 
                public void keyTyped(KeyEvent e) {  
                }  
            });  
            frame.setVisible(true);  
            try {  
                frame.wait();  
            } catch (InterruptedException e1) {  
            }  
        }  
    }
}

/*
    javac -g -cp "/Library/Java/JavaVirtualMachines/jdk1.8.0_131.jdk/Contents/Home/lib/tools.jar" 
    com/baeldung/jdi/*.java
*/