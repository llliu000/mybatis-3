/**
 *    Copyright 2009-2019 the original author or authors.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package org.apache.ibatis.scripting.xmltags;

import org.apache.ibatis.builder.SqlSourceBuilder;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.SqlSource;
import org.apache.ibatis.session.Configuration;

/**
 * 负责理动态SQL语句处理,最后封装成StaticSqlSource对象
 * DynamicSq!Source
 * @author Clinton Begin
 */
public class DynamicSqlSource implements SqlSource {

  private final Configuration configuration;
  private final SqlNode rootSqlNode;

  public DynamicSqlSource(Configuration configuration, SqlNode rootSqlNode) {
    this.configuration = configuration;
    this.rootSqlNode = rootSqlNode;
  }

  @Override
  public BoundSql getBoundSql(Object parameterObject) {
    //创建DynamicContext 对象，parameterObject是用户传入的实参
    DynamicContext context = new DynamicContext(configuration, parameterObject);
    /**
     * 通过调用rootSqlNode.apply()方法调用整个树形结构中全部SqlNode.apply()方法
     * 每个SqlNode.apply()方法都将解析得到的SQL语句片段追加到context中，
     * 最终通过 context.getSql()得到完整的 SQL 语句
     */
    rootSqlNode.apply(context);//实现动态sql
    //创建 SqlSourceBuilder，解析参数属性，并将SQL 语句中的“#{}”占位符替换成“?”占位符
    SqlSourceBuilder sqlSourceParser = new SqlSourceBuilder(configuration);
    Class<?> parameterType = parameterObject == null ? Object.class : parameterObject.getClass();
    SqlSource sqlSource = sqlSourceParser.parse(context.getSql(), parameterType, context.getBindings());

    BoundSql boundSql = sqlSource.getBoundSql(parameterObject);
    context.getBindings().forEach(boundSql::setAdditionalParameter);
    return boundSql;
  }

}
