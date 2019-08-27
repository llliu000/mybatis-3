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

import java.util.Map;

import org.apache.ibatis.parsing.GenericTokenParser;
import org.apache.ibatis.session.Configuration;

/**
 * @author Clinton Begin
 */
public class ForEachSqlNode implements SqlNode {
  public static final String ITEM_PREFIX = "__frch_";

  //用于判断循环的终止条件， ForeachSqlNode 构造方法中会创建该对象
  private final ExpressionEvaluator evaluator;
  //迭代的集合表达式
  private final String collectionExpression;
  //记录了该 foreachSqlNode 节点的子节点
  private final SqlNode contents;
  //在循环开始前要添加的字符串
  private final String open;
  //在循环结束后要添加的字符串
  private final String close;
  //循环过程中，每项之间的分隔符
  private final String separator;
  //index是当前迭代的次数， item 的值是本次选代的元素,若迭代集合是 Map ，则 index 是键， it em 是值
  private final String item;
  private final String index;
  private final Configuration configuration;

  public ForEachSqlNode(Configuration configuration, SqlNode contents, String collectionExpression, String index, String item, String open, String close, String separator) {
    this.evaluator = new ExpressionEvaluator();
    this.collectionExpression = collectionExpression;
    this.contents = contents;
    this.open = open;
    this.close = close;
    this.separator = separator;
    this.index = index;
    this.item = item;
    this.configuration = configuration;
  }

  @Override
  public boolean apply(DynamicContext context) {
    Map<String, Object> bindings = context.getBindings();
    final Iterable<?> iterable = evaluator.evaluateIterable(collectionExpression, bindings);
    if (!iterable.iterator().hasNext()) {
      return true;
    }
    boolean first = true;
    applyOpen(context);//open标签处理
    int i = 0;
    for (Object o : iterable) {
      DynamicContext oldContext = context;
      //创建 PrefixedContext，并让 context 指向该 PrefixedContext 对象
      if (first || separator == null) {
        //如果是集合的第一项，则将 PrefixedContext.prefix 初始化为空字符串
        context = new PrefixedContext(context, "");
      } else {
        //如果指定了分隔符，则PrefixedContext.prefix 初始化为指定分隔符
        context = new PrefixedContext(context, separator);
      }
      int uniqueNumber = context.getUniqueNumber();
      // Issue #709
      //处理index和item
      if (o instanceof Map.Entry) {
        //如果集合是Map类型，将集合 key和value添加到 DynamicContext.bindings 集合中保存
        @SuppressWarnings("unchecked")
        Map.Entry<Object, Object> mapEntry = (Map.Entry<Object, Object>) o;
        applyIndex(context, mapEntry.getKey(), uniqueNumber);
        applyItem(context, mapEntry.getValue(), uniqueNumber);
      } else {
        applyIndex(context, i, uniqueNumber);
        applyItem(context, o, uniqueNumber);
      }
      //处理#{},将“＃｛｝”占位符转换成“＃｛_frch_... ｝”的格式
      contents.apply(new FilteredDynamicContext(configuration, context, index, item, uniqueNumber));
      if (first) {
        first = !((PrefixedContext) context).isPrefixApplied();
      }
      context = oldContext;
      i++;
    }
    applyClose(context);//close标签处理
    context.getBindings().remove(item);
    context.getBindings().remove(index);
    return true;
  }

  private void applyIndex(DynamicContext context, Object o, int i) {
    if (index != null) {
      context.bind(index, o);
      context.bind(itemizeItem(index, i), o);
    }
  }

  private void applyItem(DynamicContext context, Object o, int i) {
    if (item != null) {
      context.bind(item, o);
      context.bind(itemizeItem(item, i), o);
    }
  }

  private void applyOpen(DynamicContext context) {
    if (open != null) {
      context.appendSql(open);
    }
  }

  private void applyClose(DynamicContext context) {
    if (close != null) {
      context.appendSql(close);
    }
  }

  private static String itemizeItem(String item, int i) {
    return ITEM_PREFIX + item + "_" + i;
  }

  /**
   * 处理“＃｛｝”占位符 ，但它并未完全解析“＃｛｝”占位符
   */
  private static class FilteredDynamicContext extends DynamicContext {
    private final DynamicContext delegate;
    private final int index;
    private final String itemIndex;
    private final String item;

    public FilteredDynamicContext(Configuration configuration,DynamicContext delegate, String itemIndex, String item, int i) {
      super(configuration, null);
      this.delegate = delegate;
      this.index = i;
      this.itemIndex = itemIndex;
      this.item = item;
    }

    @Override
    public Map<String, Object> getBindings() {
      return delegate.getBindings();
    }

    @Override
    public void bind(String name, Object value) {
      delegate.bind(name, value);
    }

    @Override
    public String getSql() {
      return delegate.getSql();
    }

    /**
     * 将“＃｛item｝”占位符转换成“＃｛_frch_item_1}”的格式，
     * 其中“ _frch ”是固定的前缀，
     * “item”与处理前的占位符样，未发生改变，
     * " 1 "则是FilteredDynamicContext产生的单调递增值；
     * 还会将“＃{itemIndex｝”占位符转换成“# {_frch_itemlndex_1｝”的格式
     */
    @Override
    public void appendSql(String sql) {
      GenericTokenParser parser = new GenericTokenParser("#{", "}", content -> {
        String newContent = content.replaceFirst("^\\s*" + item + "(?![^.,:\\s])", itemizeItem(item, index));
        if (itemIndex != null && newContent.equals(content)) {
          newContent = content.replaceFirst("^\\s*" + itemIndex + "(?![^.,:\\s])", itemizeItem(itemIndex, index));
        }
        return "#{" + newContent + "}";
      });

      delegate.appendSql(parser.parse(sql));//将解析后的 SQL 语句 片段追加到 delegate 中保存
    }

    @Override
    public int getUniqueNumber() {
      return delegate.getUniqueNumber();
    }

  }


  private class PrefixedContext extends DynamicContext {
    private final DynamicContext delegate;
    private final String prefix;
    private boolean prefixApplied;

    public PrefixedContext(DynamicContext delegate, String prefix) {
      super(configuration, null);
      this.delegate = delegate;
      this.prefix = prefix;
      this.prefixApplied = false;
    }

    public boolean isPrefixApplied() {
      return prefixApplied;
    }

    @Override
    public Map<String, Object> getBindings() {
      return delegate.getBindings();
    }

    @Override
    public void bind(String name, Object value) {
      delegate.bind(name, value);
    }

    @Override
    public void appendSql(String sql) {
      if (!prefixApplied && sql != null && sql.trim().length() > 0) {
        delegate.appendSql(prefix);
        prefixApplied = true;
      }
      delegate.appendSql(sql);
    }

    @Override
    public String getSql() {
      return delegate.getSql();
    }

    @Override
    public int getUniqueNumber() {
      return delegate.getUniqueNumber();
    }
  }

}
