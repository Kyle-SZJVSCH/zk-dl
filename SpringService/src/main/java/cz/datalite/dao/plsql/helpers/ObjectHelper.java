package cz.datalite.dao.plsql.helpers;

import cz.datalite.helpers.BooleanHelper;
import cz.datalite.helpers.ReflectionHelper;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;

/**
 * Date: 6/13/13
 * Time: 8:40 AM
 */
public final class ObjectHelper
{
    private ObjectHelper()
    {
    }


    /**
     * Vytvoření nové instance
     *
     * @param type typ vytvářeného objektu
     * @return vytvořená instance
     */
    public static <T> T newInstance(Class<T> type)
    {
        try
        {
            return type.newInstance();
        }
        catch (InstantiationException e)
        {
            throw new IllegalStateException(e);
        }
        catch (IllegalAccessException e)
        {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Ziskani getteru dane polozky
     *
     * @param aClass Trida aktualni instance
     * @param field  Polozka
     * @return getter
     */
    public static Method getFieldGetter(Class aClass, Field field)
    {
        if (aClass == null)
        {
            throw new IllegalArgumentException("Class aClass is null");
        }

        if (field == null)
        {
            throw new IllegalArgumentException("field argument is null");
        }

        String fieldName = field.getName();
        String methodName = fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1);

        try
        {
            //noinspection unchecked
            return aClass.getMethod( "get" + fieldName ) ;
        }
        catch ( NoSuchMethodException e )
        {
            try
            {
                //noinspection unchecked
                return aClass.getMethod("is" + fieldName);
            }
            catch (NoSuchMethodException e2)
            {
                try
                {
                    //noinspection unchecked
                    return aClass.getMethod("get" + methodName);
                }
                catch (NoSuchMethodException e1)
                {
                    try
                    {
                        //noinspection unchecked
                        return aClass.getMethod("is" + methodName);
                    }
                    catch (NoSuchMethodException e3)
                    {
                        if (aClass.getSuperclass() != Object.class)
                        {
                            return getFieldGetter(aClass.getSuperclass(), field);
                        }
                    }
                }
                catch (NullPointerException e4)
                {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }

    /**
     * Ziskani getteru dane polozky
     *
     * @param aClass Trida aktualni instance
     * @param field  Polozka
     * @return getter
     */
    public static Method getFieldSetter(Class aClass, Field field)
    {
        if (aClass == null)
        {
            throw new IllegalArgumentException("Class aClass is null");
        }

        if (field == null)
        {
            throw new IllegalArgumentException("field argument is null");
        }

        String fieldName = field.getName();
        String methodName = fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1);

        try
        {
            //noinspection unchecked
            return aClass.getMethod( "set" + fieldName, field.getType() ) ;
        }
        catch (NoSuchMethodException e)
        {
            try
            {
                //noinspection unchecked
                return aClass.getMethod( "set" + methodName, field.getType() ) ;
            }
            catch (NoSuchMethodException e1)
            {
                if (aClass.getSuperclass() != Object.class)
                {
                    return getFieldSetter(aClass.getSuperclass(), field);
                }
            }
            catch (NullPointerException e2)
            {
                e.printStackTrace();
            }
        }

        return null;
    }

    public static Object getValue(String fieldName, Object obj)
    {
        if (obj == null)
        {
            return null;
        }

        try
        {
            Field field = ReflectionHelper.getDeclaredField(obj.getClass(), fieldName);

            if (field.isAccessible())
            {
                return field.get(obj);
            }
            else
            {
                Method m = getFieldGetter( field.getDeclaringClass(), field ) ;

                return m.invoke( obj ) ;
            }
        }
        catch (NoSuchFieldException e)
        {
            throw new IllegalStateException(e);
        }
        catch (InvocationTargetException e)
        {
            throw new IllegalStateException(e);
        }
        catch (IllegalAccessException e)
        {
            throw new IllegalStateException(e);
        }
    }

    public static <T> T getValue(String fieldName, Object obj, Class<T> returnType)
    {
        return ObjectHelper.extractFromObject(getValue(fieldName, obj), returnType);
    }

    public static void setValue(String fieldName, Object obj, Object value)
    {
        try
        {
            Field field = ReflectionHelper.getDeclaredField(obj.getClass(), fieldName);

            if (field.isAccessible())
            {
                field.set( obj, value ) ;
            }
            else
            {
                Method m = getFieldSetter( field.getDeclaringClass(), field ) ;

                m.invoke( obj, value ) ;
            }
        }
        catch (NoSuchFieldException e)
        {
            throw new IllegalStateException(e);
        }
        catch (InvocationTargetException e)
        {
            throw new IllegalStateException(e);
        }
        catch (IllegalAccessException e)
        {
            throw new IllegalStateException(e);
        }
    }



    /**
     * Extrahovaní hodnoty
     *
     * @param value      Hodnota
     * @param returnType Navratový typ pozadovane hodnoty
     * @return převedená hodnota
     */
    public static <T> T extractFromObject(Object value, Class<T> returnType)
    {
        if ((returnType == Integer.class) || (returnType == int.class))
        {
            //noinspection unchecked
            return (T) extractInteger(value);
        }
        else if ((returnType == Long.class) || (returnType == long.class))
        {
            //noinspection unchecked
            return (T) extractLong(value);
        }
        else if ((returnType == Double.class) || (returnType == double.class))
        {
            //noinspection unchecked
            return (T) extractDouble(value);
        }
        else if (returnType == BigDecimal.class)
        {
            //noinspection unchecked
            return (T) extractBigDecimal(value);
        }
        else if ((returnType == Boolean.class) || (returnType == boolean.class))
        {
            //noinspection unchecked
            return (T) extractBoolean(value);
        }
        else if (returnType == String.class)
        {
            //noinspection unchecked
            return (T) extractString(value);
        }
        else if ( returnType.isEnum() )
        {
            return (T)EnumHelper.getEnumValue( returnType, extractString( value ) ) ;
        }

        //noinspection unchecked
        return (T) value;
    }


    /**
     * @param value převáděná hodnota
     * @return převedená hodnota
     */
    public static String extractString(Object value)
    {
        if (value instanceof Boolean)
        {
            return ((Boolean) value) ? "A" : "N";
        }

        return (value != null) ? String.valueOf(value) : null;
    }

    /**
     * @param value převáděná hodnota
     * @return převedená hodnota
     */
    public static Long extractLong(Object value)
    {
        if (value instanceof Long)
        {
            return (Long) value;
        }
        else if (value instanceof Integer)
        {
            return (long) (Integer) value;
        }
        else if (value instanceof BigDecimal)
        {
            return ((BigDecimal) value).longValue();
        }

        return (value != null) ? Long.parseLong(String.valueOf(value)) : null;
    }

    /**
     * @param value převáděná hodnota
     * @return převedená hodnota
     */
    public static Double extractDouble(Object value)
    {
        if (value instanceof Double)
        {
            return (Double) value;
        }
        else if (value instanceof Integer)
        {
            return (double) (Integer) value;
        }
        else if (value instanceof BigDecimal)
        {
            return ((BigDecimal) value).doubleValue();
        }

        return (value != null) ? Double.parseDouble(String.valueOf(value)) : null;
    }

    /**
     * @param value převáděná hodnota
     * @return převedená hodnota
     */
    public static Integer extractInteger(Object value)
    {
        if (value instanceof Long)
        {
            return ((Long) value).intValue();
        }
        else if (value instanceof BigDecimal)
        {
            return ((BigDecimal) value).intValue();
        }
        else if (value instanceof Integer)
        {
            return ((Integer) value);
        }

        return (value != null) ? Integer.parseInt(String.valueOf(value)) : null;
    }

    /**
     * @param value převáděná hodnota
     * @return převedená hodnota
     */
    public static BigDecimal extractBigDecimal(Object value)
    {
        if (value instanceof Long)
        {
            return new BigDecimal((Long) value);
        }
        else if (value instanceof BigDecimal)
        {
            return ((BigDecimal) value);
        }
        else if (value instanceof Integer)
        {
            return new BigDecimal((Integer) value);
        }

        return (value != null) ? new BigDecimal(String.valueOf(value)) : null;
    }

    /**
     * @param value převáděná hodnota
     * @return převedená hodnota
     */
    public static Boolean extractBoolean(Object value)
    {
        if (value instanceof Long)
        {
            return (Long) value != 0;
        }
        else if (value instanceof BigDecimal)
        {
            return !BigDecimal.ZERO.equals(value);
        }
        else if (value instanceof Integer)
        {
            return (Integer) value != 0;
        }
        else if ( value instanceof String )
        {
            return BooleanHelper.isTrue((String) value) ;
        }

        return (value != null) ? BooleanHelper.isTrue(String.valueOf(value)) : null;
    }
}
