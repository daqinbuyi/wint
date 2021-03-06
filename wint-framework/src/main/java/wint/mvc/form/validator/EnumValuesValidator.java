package wint.mvc.form.validator;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import wint.lang.utils.CollectionUtil;
import wint.lang.utils.StringUtil;

public class EnumValuesValidator extends AbstractValidator {

	private Set<String> values;
	
	@Override
	protected boolean validate(String fieldValue) {
		if (StringUtil.isEmpty(fieldValue)) {
			return true;
		}
		if (CollectionUtil.isEmpty(values)) {
			return false;
		}
		return values.contains(fieldValue);
	}

	public void setValues(String enumValues) {
		if (StringUtil.isEmpty(enumValues)) {
			return;
		}
		List<String> parts = StringUtil.splitTrim(enumValues, "[,;]");
		values = new HashSet<String>(parts);
	}

}
