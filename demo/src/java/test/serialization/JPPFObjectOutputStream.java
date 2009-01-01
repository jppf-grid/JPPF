/*
 * Java Parallel Processing Framework.
 *  Copyright (C) 2005-2009 JPPF Team. 
 * http://www.jppf.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 	 http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package test.serialization;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;

import org.apache.commons.logging.*;
import org.jppf.classloader.ResourceProvider;
import org.objectweb.asm.*;
import org.objectweb.asm.util.TraceClassVisitor;

/**
 * 
 * @author Laurent Cohen
 */
public class JPPFObjectOutputStream extends ObjectOutputStream
{
	/**
	 * Logger for this class.
	 */
	private static Log log = LogFactory.getLog(JPPFObjectOutputStream.class);
	/**
	 * Determines whether debug-level logging is enabled.
	 */
	private static boolean debugEnabled = log.isDebugEnabled();
	/**
	 * Set of already defined classes.
	 */
	private Set<String> definedClasses = new HashSet<String>();
	/**
	 * Mapping of parent class loaders to their custom children.
	 */
	private Map<ClassLoader, JPPFURLClassLoader> parentMap = new HashMap<ClassLoader, JPPFURLClassLoader>();
	/**
	 * Enable object override.
	 */
	private boolean enableOverride = true;

	/**
	 * Provide a way for subclasses that are completely reimplementing ObjectOutputStream to not have to
	 * allocate private data just used by this implementation of ObjectOutputStream.
	 * If there is a security manager installed, this method first calls the security manager's checkPermission
	 * method with a SerializablePermission("enableSubclassImplementation") permission to ensure it's ok to enable subclassing.
	 * @throws IOException .
	 * @throws SecurityException if a security manager exists and its checkPermission method denies enabling subclassing.
	 */
	public JPPFObjectOutputStream() throws IOException, SecurityException
	{
		super();
	}

	/**
	 * Creates an ObjectOutputStream that writes to the specified OutputStream.
	 * This constructor writes the serialization stream header to the underlying stream;
	 * callers may wish to flush the stream immediately to ensure that constructors for receiving ObjectInputStreams will not block when reading the header.
	 * If there is a security manager installed, this method first calls the security manager's checkPermission
	 * method with a SerializablePermission("enableSubclassImplementation") permission to ensure it's ok to enable subclassing.
	 * @param out the output stream to write to.
	 * @throws IOException .
	 */
	public JPPFObjectOutputStream(OutputStream out) throws IOException
	{
		super(out);
		enableReplaceObject(true);
	}

	/**
	 * 
	 * @param obj .
	 * @throws IOException .
	 * @see java.io.ObjectOutputStream#writeObjectOverride(java.lang.Object)
	 */
	protected void writeObjectOverride(Object obj) throws IOException
	{
		super.writeObjectOverride(obj);
	}

	/**
	 * Write the specified object to the ObjectOutputStream.
	 * @param obj the object to write.
	 * @return the alternate object that replaced the specified one.
	 * @throws IOException if an error occurs while writing the object ot the stream.
	 * @see java.io.ObjectOutputStream#replaceObject(java.lang.Object)
	 */
	public Object replaceObject(Object obj) throws IOException
	{
		if ((obj == null) || (obj instanceof Class) || (obj instanceof Externalizable) || (obj instanceof Serializable))
			return obj;
		return replace(obj);
	}

	/**
	 * Replace a non-serializable object with a serializable copy.
	 * The class of the copy is generated on the fly as a serializable subclass of the original object's class. 
	 * @param obj the object to replace.
	 * @return an object instance of a serializable subclass
	 */
	private Object replace(Object obj)
	{
		Object result = null;
		try
		{
			ResourceProvider rp = new ResourceProvider();
			String s = obj.getClass().getName();
			String name = s.replace('.', '/');
			ClassLoader cl = obj.getClass().getClassLoader();
			byte[] b = rp.getResource(name + ".class", cl);
			ClassReader classReader = new ClassReader(b);
			JPPFClassWriter classWriter = new JPPFClassWriter(classReader);
			PrintWriter pw = new PrintWriter(new FileWriter("testgen.txt"));
			TraceClassVisitor tcv = new TraceClassVisitor(classWriter, pw);
			//classWriter.visit(Opcodes.V1_5, classReader.getAccess(), name, null, classReader.getSuperName(), classReader.getInterfaces());
			//classWriter.visitEnd();
			//tcv.visit(Opcodes.V1_5, classReader.getAccess(), name, null, classReader.getSuperName(), classReader.getInterfaces());
			//tcv.visitEnd();
			classReader.accept(tcv, 0);
			b = classWriter.toByteArray();
			JPPFURLClassLoader child = parentMap.get(cl);
			if (child == null)
			{
				child = new JPPFURLClassLoader(cl);
				parentMap.put(cl, child);
			}
			Class clazz = child.doDefineClass("jppfgen." + s, b);
			result = doCopy(obj, clazz);
		}
		catch(Exception e)
		{
			log.error(e.getMessage(), e);
			throw new RuntimeException(e);
		}
		return result;
	}

	/**
	 * Copy the fields of the specified object into a new instance of the specified class.
	 * @param obj the object to copy.
	 * @param newClass the class from which to create a new instance.
	 * @return a new instance of the specified class.
	 * @throws Exception if any error occurs.
	 */
	private Object doCopy(Object obj, Class newClass) throws Exception
	{
		Class clazz = obj.getClass();
		Object newObj = newClass.newInstance();
		List<Class> path = new ArrayList<Class>();
		List<Field> fields = new ArrayList<Field>();
		for (Class c = clazz; !Object.class.equals(c);)
		{
			Field[] declaredFields = c.getDeclaredFields();
			for (Field f: declaredFields)
			{
				int n = f.getModifiers();
				if (!Modifier.isStatic(n) && !Modifier.isTransient(n))
				{
					f.setAccessible(true);
					Object value = f.get(obj);
					Field newField = newClass.getField(f.getName());
					newField.setAccessible(true);
					newField.set(newObj, value);
				}
			}
		}
		return newObj;
	}
}
