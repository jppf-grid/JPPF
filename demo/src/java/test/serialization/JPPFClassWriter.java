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

import org.objectweb.asm.*;

/**
 * 
 * @author Laurent Cohen
 */
public class JPPFClassWriter extends ClassWriter
{
	/**
	 * Constructs a new ClassWriter object and enables optimizations for "mostly add" bytecode transformations.
	 * @param classReader the ClassReader used to read the original class.
	 */
	public JPPFClassWriter(ClassReader classReader)
	{
		super(classReader, 0);
		int i= 0;
	}

	/**
	 * 
	 * @param version the class version.
	 * @param access the class's access flags. This parameter also indicates if the class is deprecated.
	 * @param name the internal name of the class.
	 * @param signature the signature of this class.
	 * @param superName the internal of name of the super class.
	 * @param interfaces the internal names of the class's interfaces.
	 * @see org.objectweb.asm.ClassWriter#visit(int, int, java.lang.String, java.lang.String, java.lang.String, java.lang.String[])
	 */
	public void visit(int version, int access, String name, String signature, String superName, String[] interfaces)
	{
		String serializableName = "java/io/Serializable";
		String[] inf = null;
		if (interfaces == null) inf = new String[] { serializableName };
		else
		{
			int n = interfaces.length;
			inf = new String[n + 1];
			for (int i=0; i<n; i++) inf[i] = interfaces[i];
			inf[n] = serializableName;
		}
		super.visit(version, access, "jppfgen/" + name, signature, superName, inf);
	}

	/**
	 * Visit a field.
   * @param access - the field's access flags (see Opcodes). This parameter also indicates if the field is synthetic and/or deprecated.
	 * @param name - the field's name.
	 * @param desc - the field's descriptor (see Type).
	 * @param signature - the field's signature. May be null if the field's type does not use generic types.
	 * @param value - the field's initial value. This parameter is only used for static fields. 
	 * @return a visitor to visit field annotations and attributes, or null if this class visitor is not interested in visiting these annotations and attributes.
	 * @see org.objectweb.asm.ClassWriter#visitField(int, java.lang.String, java.lang.String, java.lang.String, java.lang.Object)
	 */
	public FieldVisitor visitField(int access, String name, String desc, String signature, Object value)
	{
		return null;
		//return super.visitField(access, name, desc, signature, value);
	}

	/**
	 * Visit a method.
	 * @param access - the method's access flags (see Opcodes). This parameter also indicates if the method is synthetic and/or deprecated.
	 * @param name - the method's name.
	 * @param desc - the method's descriptor (see Type).
	 * @param signature - the method's signature. May be null if the method parameters, return type and exceptions do not use generic types.
	 * @param exceptions - the internal names of the method's exception classes (see getInternalName). May be null. 
	 * @return an object to visit the byte code of the method, or null if this class visitor is not interested in visiting the code of this method.
	 * @see org.objectweb.asm.ClassWriter#visitMethod(int, java.lang.String, java.lang.String, java.lang.String, java.lang.String[])
	 */
	public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions)
	{
		return super.visitMethod(access, name, desc, signature, exceptions);
	}
}
