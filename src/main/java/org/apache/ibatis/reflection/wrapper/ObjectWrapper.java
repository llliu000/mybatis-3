/**
 * Copyright 2009-2019 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.ibatis.reflection.wrapper;

import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.factory.ObjectFactory;
import org.apache.ibatis.reflection.property.PropertyTokenizer;

import java.util.List;

/**
 * @author Clinton Begin
 */
public interface ObjectWrapper {

  /**
   * 如果 ObjectWrapper 中封装的是普通的 Bean 对象，则 调用相应属性的相应 getter 方法，
   * 如果封装的是集合类，则获取指定 key 或下标对应的 value
   */

  Object get(PropertyTokenizer prop);

  /**
   * 如果 objectWrapper 中封装的是普通的 Bean 对象 则调用相应属性的相应 setter 方法
   * 如果封装的是集合类，则设置指定 key 或下标对应的 value
   */
  void set(PropertyTokenizer prop, Object value);

  /**
   * 查找属性表达式指定的属性，第二个参数表示是否忽略属性表达式中的下画线
   */
  String findProperty(String name, boolean useCamelCaseMapping);

  /**
   * 查找可写属性的名称集合
   */
  String[] getGetterNames();

  /**
   * 查找可读属性的名称集合
   */
  String[] getSetterNames();

  /**
   * 解析属性表达式指定属性的 setter方法的参数类型
   */
  Class<?> getSetterType(String name);

  /**
   * 解析属性表达式指定属性的 getter方法的返回值类型
   */
  Class<?> getGetterType(String name);

  //判断属性表达式指定属性是否有 getter/setter 方法
  boolean hasSetter(String name);
  boolean hasGetter(String name);

  /**
   * 为属性表达式指定的属性创建相应的 MetaObject 对象
   */
  MetaObject instantiatePropertyValue(String name, PropertyTokenizer prop, ObjectFactory objectFactory);

  /**
   * 判断包装的对象是否为集合
   */
  boolean isCollection();

  //集合方法调用
  void add(Object element);
  <E> void addAll(List<E> element);

}
