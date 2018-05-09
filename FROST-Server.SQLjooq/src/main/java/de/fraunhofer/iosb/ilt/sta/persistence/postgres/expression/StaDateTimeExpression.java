/*
 * Copyright (C) 2016 Fraunhofer Institut IOSB, Fraunhoferstr. 1, D 76131
 * Karlsruhe, Germany.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.fraunhofer.iosb.ilt.sta.persistence.postgres.expression;

import com.querydsl.core.types.ConstantImpl;
import com.querydsl.core.types.dsl.DateTimeExpression;
import com.querydsl.core.types.dsl.DateTimeTemplate;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.StringTemplate;
import de.fraunhofer.iosb.ilt.sta.persistence.postgres.PgExpressionHandler;
import de.fraunhofer.iosb.ilt.sta.query.expression.constant.DurationConstant;
import java.sql.Timestamp;
import org.jooq.Condition;
import org.jooq.Field;

/**
 * @author scf
 */
public class StaDateTimeExpression extends TimeExpression {

    public static DateTimeExpression createDateTimeExpression(final Field<Timestamp> source) {
        return new DateTimeExpression<Timestamp>(source);
    }
    /**
     * Flag indicating that the original time given was in utc.
     */
    private boolean utc = true;
    private final Field<Timestamp> source;

    /**
     *
     * @param ts The constant timestamp.
     * @param utc Flag indicating that the original time given was in utc.
     */
    public StaDateTimeExpression(final Timestamp ts, boolean utc) {
        source = createDateTimeExpression(ConstantImpl.create(ts));
        this.utc = utc;
    }

    public StaDateTimeExpression(Field source) {
        this.source = source;
    }

    /**
     * @return Flag indicating that the original time given was in utc.
     */
    public boolean isUtc() {
        return utc;
    }

    public DateTimeExpression<Timestamp> getExpression() {
        return source;
    }

    @Override
    public Class getType() {
        return DurationConstant.class;
    }

    private Field<?> specificOp(String op, StaDurationExpression other) {
        switch (op) {
            case "+":
            case "-":
                String template = "(({0})::timestamp " + op + " ({1})::interval)";
                DateTimeTemplate<Timestamp> expression = Expressions.dateTimeTemplate(Timestamp.class, template, source, other.duration);
                return new StaDateTimeExpression(expression);

            default:
                throw new UnsupportedOperationException("Can not mul or div a DateTime with a " + other.getClass().getName());
        }
    }

    private Field<?> specificOp(String op, StaDateTimeExpression other) {
        switch (op) {
            case "-":
                String template = "(({0})::timestamp " + op + " ({1})::timestamp)";
                StringTemplate expression = Expressions.stringTemplate(template, source, other.getExpression());
                return new StaDurationExpression(expression);

            default:
                throw new UnsupportedOperationException("Can not add, mul or div two DateTimes.");
        }
    }

    @Override
    public Field<Timestamp> simpleOp(String op, Field<?> other) {
        if (other instanceof StaDurationExpression) {
            return specificOp(op, (StaDurationExpression) other);
        }
        if (other instanceof StaDateTimeExpression) {
            return specificOp(op, (StaDateTimeExpression) other);
        }
        throw new UnsupportedOperationException("Can not add, sub, mul or div a DateTime with a " + other.getClass().getName());
    }

    private Condition specificOpBool(String op, StaDateTimeExpression other) {
        DateTimeExpression<Timestamp> t1 = source;
        DateTimeExpression<Timestamp> t2 = other.source;
        switch (op) {
            case "=":
                return t1.eq(t2);

            case ">":
                return t1.gt(t2);

            case ">=":
                return t1.goe(t2);

            case "<":
                return t1.lt(t2);

            case "<=":
                return t1.loe(t2);

            case "a":
                return t1.gt(t2);

            case "b":
                return t1.lt(t2);

            case "c":
                throw new UnsupportedOperationException("First parameter of contains must be an interval.");

            case "m":
                return t1.eq(t2);

            case "o":
                return t1.eq(t2);

            case "s":
                return t1.eq(t2);

            case "f":
                return t1.eq(t2);

            default:
                throw new UnsupportedOperationException("Unknown boolean operation: " + op);
        }
    }

    private Condition specificOpBool(String op, StaTimeIntervalExpression other) {
        DateTimeExpression<Timestamp> t1 = source;
        DateTimeExpression s2 = PgExpressionHandler.checkType(DateTimeExpression.class, other.getStart(), false);
        DateTimeExpression e2 = PgExpressionHandler.checkType(DateTimeExpression.class, other.getEnd(), false);
        switch (op) {
            case "=":
                return t1.eq(s2).and(t1.eq(e2));

            case ">":
                return t1.goe(e2).and(t1.gt(s2));

            case ">=":
                return t1.goe(e2);

            case "<":
                return t1.lt(s2);

            case "<=":
                return t1.loe(s2);

            case "a":
                return t1.goe(e2).and(t1.gt(s2));

            case "b":
                return t1.lt(s2);

            case "c":
                throw new UnsupportedOperationException("First parameter of contains must be an interval.");

            case "m":
                return t1.eq(s2).or(t1.eq(e2));

            case "o":
                return t1.eq(s2).or(s2.loe(t1).and(e2.gt(t1)));

            case "s":
                return t1.eq(s2);

            case "f":
                return t1.eq(e2);

            default:
                throw new UnsupportedOperationException("Unknown boolean operation: " + op);
        }
    }

    @Override
    public Condition simpleOpBool(String op, Field<?> other) {
        if (other instanceof StaDateTimeExpression) {
            return specificOpBool(op, (StaDateTimeExpression) other);
        }
        if (other instanceof StaTimeIntervalExpression) {
            return specificOpBool(op, (StaTimeIntervalExpression) other);
        }
        throw new UnsupportedOperationException("Can not compare between Duration and " + other.getClass().getName());
    }

}
