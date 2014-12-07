using System;
using System.Collections.Generic;
using System.IO;
using System.Runtime.Serialization;
using System.Runtime.Serialization.Formatters.Binary;
using System.Reflection;
using System.Threading;
using net.sf.jni4net;

namespace org.jppf.dotnet {
  /// <summary>
  /// This class provides the functionality to serialize and desrialize a .Net task,
  /// and execute it on demand. It also handles cancel requests from the Java side.
  /// </summary>
  public class DotnetSerializer {
    /// <summary>temporary reference to the current thread executing the task</summary>
    private Thread currentThread = null;
    /// <summary>temporary reference to the task being executed</summary>
    private BaseDotnetTask task = null;
    /// <summary>Internal flag used in the scope of the task execution</summary>
    private bool cancelled = false;
    /// <summary>Internal flag used in the scope of the task execution</summary>
    private bool timeout = false;

    public DotnetSerializer() {
    }

    /// <summary>Serialize the specified object to binary format</summary>
    /// <param name="obj">the object to serialize</param>
    /// <returns>a <c>byte[]</c> holding the serialized object</returns>
    public byte[] Serialize(object obj) {
      MemoryStream stream = new MemoryStream();
      BinaryFormatter serializer = new BinaryFormatter();
      serializer.Serialize(stream, obj);
      stream.Close();
      return stream.ToArray();
    }

    /// <summary>Deserialize the specified object from binary format</summary>
    /// <param name="bytes">a <c>byte[]</c> holding the serialized object</param>
    /// <returns>the deserialized object</returns>
    public object Deserialize(byte[] bytes) {
      Stream stream = new MemoryStream(bytes);
      BinaryFormatter deserializer = new BinaryFormatter();
      deserializer.Binder = new CustomizedBinder();
      object o = deserializer.Deserialize(stream);
      stream.Close();
      return o;
    }

    /// <summary>deserialize the specified <c>byte[]</c> into a <c>BaseDotnetTask</c>,
    /// invoke its <c>Execute()</c> method, then serialize its new state</summary>
    /// <param name="bytes"></param>
    /// <returns>a <c>bytes[]</c> holding the state the task after execution</returns>
    public byte[] Execute(byte[] bytes) {
      if (cancelled) return bytes;
      currentThread = Thread.CurrentThread;
      try {
        //System.Console.WriteLine("deserialized object: " + obj);
        task = (BaseDotnetTask) Deserialize(bytes);
        if (cancelled) return bytes;
        try {       
          task.Execute();
        } catch (ThreadInterruptedException e) {
          Console.WriteLine("" + e);
        } catch (Exception e) {
          task.Exception = e;
        }
        if (cancelled) return bytes;
        return Serialize(task);
      } finally {
        currentThread = null;
        task = null;
      }
      return bytes;
    }

    /// <summary>Attempt to cancel the task during or before its execution.
    /// If the task is running, an attempt is made to interrupt the thread executing it.
    /// In any case the task's cancelled flag is set to true.
    /// </summary>
    public void Cancel() {
      if (cancelled) return;
      try {
        cancelled = true;
        if (task != null) {
          task.Cancelled = true;
          if (currentThread != null) currentThread.Interrupt();
          task.OnCancel();
        }
      } catch (Exception e) {
        Console.WriteLine("" + e);
        Console.WriteLine(e.StackTrace);
      }
    }
    /// <summary>Attempt to cancel the task when it times out.
    /// If the task is running, an attempt is made to interrupt the thread executing it.
    /// In any case the task's timeout flag is set to true.
    /// </summary>
    public void Timeout() {
      if (cancelled) return;
      try {
        timeout = true;
        if (task != null) {
          task.TimedOut = true;
          if (currentThread != null) currentThread.Interrupt();
          task.OnTimeout();
        }
      } catch (Exception e) {
        Console.WriteLine("" + e);
        Console.WriteLine(e.StackTrace);
      }
    }
  }

  sealed class CustomizedBinder : SerializationBinder {
    private static bool initDone = Init();

    public override Type BindToType(string assemblyName, string typeName) {
      return Type.GetType(String.Format("{0}, {1}", typeName, assemblyName));
    }

    /// <summary>Load all the assemblies known to the jni4net bridge.
    /// This one-time operation is a workaround to avoid the 'Unable to find assembly' error upon deserialization.</summary>
    /// <returns><c>true</c> if the assemblies could be loaded, <c>false</c> otherwise</returns>
    private static bool Init() {
      if (!initDone) {
        initDone = true;
        IList<Assembly> knownAssemblies = Bridge.KnownAssemblies;
        if (knownAssemblies != null) {
          foreach (Assembly a in knownAssemblies) {
            if (a != null) Assembly.Load(a.FullName);
          }
        } else {
          Console.WriteLine("found no assembly known to jni4net bridge");
        }
      }
      return true;
    }
  }
}
