package plugins.harmonizationPlugin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CreatePotentialTerms
{
	/*
	 * Given a list 'start' with tokens (a predictor). Return all possible
	 * different lists in which the tokens appear in the same order, possibly
	 * concatenated. Each list can be considered as a set of 'building blocks'.
	 */
	public static List<List<String>> getTermsLists(List<String> start)
	{
		List<List<String>> result = new ArrayList<List<String>>();
		if (0 < start.size())
		{
			for (int pos = 0; pos < start.size(); pos++)
			{
				List<String> left = asOneElement(start.subList(0, pos + 1));
				result.addAll(combine(left, getTermsLists(start.subList(pos + 1, start.size()))));
			}
		}
		return result;
	}

	/*
	 * Concatenate all list elements to one string. Return a list which has only
	 * one element: that string.
	 */
	private static List<String> asOneElement(List<String> list)
	{
		String result = "";
		for (int i = 0; i < list.size(); i++)
			result = result + (0 == i ? "" : " ") + list.get(i);
		return new ArrayList<String>(Arrays.asList(result));
	}

	/*
	 * If termsLists empty, then return a list with only prefix in it. Else
	 * prepend prefix to each element in termsLists, and return the result.
	 */
	private static List<List<String>> combine(List<String> prefix, List<List<String>> termsLists)
	{
		List<List<String>> result = new ArrayList<List<String>>();
		if (0 == termsLists.size()) result.add(prefix);
		else
		{
			for (int i = 0; i < termsLists.size(); i++)
			{
				List<String> newResult = new ArrayList<String>();
				newResult.addAll(prefix);
				newResult.addAll(termsLists.get(i));
				result.add(newResult);
			}
		}
		return result;
	}

	public static void main(String[] args)
	{
		List<String> example = new ArrayList<String>(Arrays.asList("parental", "diabetes", "mellitus", "type2", "test"));
		System.out.println(getTermsLists(example));
	}
}