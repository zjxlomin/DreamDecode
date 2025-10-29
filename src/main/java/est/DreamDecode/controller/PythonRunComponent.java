package est.DreamDecode.controller;

import lombok.RequiredArgsConstructor;
import org.python.core.PyFunction;
import org.python.core.PyInteger;
import org.python.core.PyObject;
import org.python.util.PythonInterpreter;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class PythonRunComponent {
    private static PythonInterpreter intPre;

    @RequestMapping(value = "/test", method = RequestMethod.GET)
    public String getTest(){
        intPre = new PythonInterpreter();
        intPre.execfile("src/main/resources/static/python/test.py"); // test.py 경로

        // web 으로 확인
        PyFunction pyFuntion = (PyFunction) intPre.get("testFunc2", PyFunction.class);
        int a = 10, b = 20;
        PyObject pyobj = pyFuntion.__call__(new PyInteger(a), new PyInteger(b));
        // System.out.println(pyobj.toString());

        return pyobj.toString();
    }
}
