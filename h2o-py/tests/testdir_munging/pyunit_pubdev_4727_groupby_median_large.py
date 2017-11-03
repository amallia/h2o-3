from __future__ import print_function
import sys
sys.path.insert(1,"../../")
import h2o
from tests import pyunit_utils
import numpy as np
from random import randint

# global dictionaries storing model answers
g_iris_setosa_sepal_len=dict()
g_iris_versicolor_sepal_wid=dict()
g_iris_virginica_petal_wid=dict()
g_iris_versicolor_petal_len_NA_ignore=dict()
g_iris_versicolor_petal_len_NA_rm=dict()


def group_by_all():
    """
    I am testing the groupby median function in PUBDEV_4727.
    """

    # generate random dataset with factor column and real columns
    row_num_max = 100000 # 100000
    row_num_min = 100
    enumLow = 5
    enumHigh = 30
    enumVals = randint(enumLow, enumHigh)   # total number of groupby class
    pIndex = []
    pNum = []
    numpMedian = [] # store python median calculated for each groupby class
    for index in range(enumVals):
        rowNum = randint(row_num_min, row_num_max)
        indexList = [index]*rowNum
        numList = np.random.rand(rowNum,1)
        numpMedian.append(list(np.median(numList, axis=0))[0])
        pIndex.extend(indexList)
        pNum.extend(numList)

    # generate random H2OFrame
    newOrder = np.random.permutation(len(pIndex))
    python_lists = []
    for index in range(len(pIndex)):
        temp = [pIndex[newOrder[index]], pNum[newOrder[index]][0]]
        python_lists.append(temp)
    h2oframe= h2o.H2OFrame(python_obj=python_lists, column_types=["enum","int"], column_names=["factors", "numerics"])

    # calculate median for each groupby class
    groupedMedianF = h2oframe.group_by(["factors"]).median(na='rm').mean(na='rm').sum(na="all").count(na="rm").get_frame()

    groupbyMedian = [0]*len(numpMedian) # extract groupby median to compare with python median
    for rowIndex in range(enumVals):
        groupbyMedian[int(groupedMedianF[rowIndex,0])] = groupedMedianF[rowIndex,'median_numerics']

    print("H2O Groupby median is {0}".format(groupbyMedian))
    print("Numpy median is {0}".format(numpMedian))
    assert pyunit_utils.equal_two_arrays(groupbyMedian, numpMedian, 1e-12, 1e-12), "H2O groupby median and numpy " \
                                                                                   "median is different."

if __name__ == "__main__":
    pyunit_utils.standalone_test(group_by_all)
else:
    group_by_all()
